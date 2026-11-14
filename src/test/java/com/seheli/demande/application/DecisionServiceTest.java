package com.seheli.demande.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.audit.domain.EntreeJournal;
import com.seheli.audit.domain.TypeActeur;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.StatutEcheance;
import com.seheli.demande.JustificatifDemande;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.domain.DecisionImpossibleException;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandesFixtures;
import com.seheli.demande.domain.MotifRefusInvalideException;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.demande.domain.TransitionInterditeException;
import com.seheli.identite.domain.TypeDocument;

class DecisionServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-13T09:30:00Z");

    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final JournalEnMemoire entrees = new JournalEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(entrees);
    private final List<Object> evenements = new ArrayList<>();
    private final DecisionsDeTest decisions = new DecisionsDeTest(demandes, journal, evenements::add,
            Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    private final DecisionService service = decisions.service;

    private Demande enAnalyse() {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.EN_ANALYSE);
        demandes.enregistrer(d);
        decisions.emploiCdi(d.clientId());
        return d;
    }

    private List<EntreeJournal> journalDe(Demande d) {
        return journal.historique(JournalDossier.objet(d.id()));
    }

    @Test
    void accepter_change_le_statut_et_apparait_au_journal() {
        Demande d = enAnalyse();

        service.accepter(d.id(), "analyste");

        Demande relue = demandes.parId(d.id()).orElseThrow();
        assertThat(relue.statut()).isEqualTo(StatutDossier.ACCEPTEE);
        assertThat(relue.statutChangeLe()).isEqualTo(MAINTENANT);
        assertThat(relue.decision()).hasValueSatisfying(dec -> assertThat(dec.analyste()).isEqualTo("analyste"));
        assertThat(demandes.verrous()).isEqualTo(1);

        assertThat(journalDe(d)).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("ACCEPTATION");
            assertThat(e.avant()).contains("EN_ANALYSE");
            assertThat(e.apres()).contains("ACCEPTEE");
            assertThat(e.acteur().type()).isEqualTo(TypeActeur.BACK_OFFICE);
            assertThat(e.acteur().identifiant()).isEqualTo("analyste");
            assertThat(e.le()).isEqualTo(MAINTENANT);
        });
        assertThat(evenements).containsExactly(new StatutDossierChange(d.id(), d.clientId(), StatutDossier.EN_ANALYSE,
                StatutDossier.ACCEPTEE, MAINTENANT));
    }

    @Test
    void accepter_etablit_l_offre_et_le_contrat_avec_ses_echeances() {
        Demande d = enAnalyse();

        service.accepter(d.id(), "analyste");

        assertThat(decisions.contratService.parDemande(d.id())).hasValueSatisfying(c -> {
            assertThat(c.clientId()).isEqualTo(d.clientId());
            assertThat(c.prix().prixVente()).isEqualTo(Montant.de(20_800));
            assertThat(c.apport()).isEqualTo(Montant.de(8_320));
            assertThat(c.etabliLe()).isEqualTo(MAINTENANT);
            assertThat(c.echeances()).hasSize(6).allSatisfy(e -> {
                assertThat(e.montant()).isEqualTo(Montant.de(2_080));
                assertThat(e.statut()).isEqualTo(StatutEcheance.A_VENIR);
            });
            // Acceptée le 13/11, apport attendu avant le 20/11 : première paie suivante le 25/11.
            assertThat(c.echeances().getFirst().date()).isEqualTo(LocalDate.of(2026, 11, 25));
            assertThat(journalDe(d)).singleElement()
                    .satisfies(e -> assertThat(e.detail()).contains("Contrat " + c.numero()));
        });
    }

    @Test
    void sans_emploi_le_contrat_ne_peut_pas_etre_etabli() {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.EN_ANALYSE);
        demandes.enregistrer(d);

        assertThatThrownBy(() -> service.accepter(d.id(), "analyste"))
                .isInstanceOf(EmploiNonDeclareException.class);
        assertThat(decisions.contrats.tous()).isEmpty();
        assertThat(journalDe(d)).isEmpty();
        assertThat(evenements).isEmpty();
    }

    @Test
    void refuser_n_etablit_pas_de_contrat() {
        Demande d = enAnalyse();

        service.refuser(d.id(), "analyste", "Ancienneté trop faible");

        assertThat(decisions.contrats.tous()).isEmpty();
    }

    @Test
    void refuser_conserve_le_motif_au_dossier_et_au_journal() {
        Demande d = enAnalyse();

        service.refuser(d.id(), "analyste", "Ancienneté trop faible");

        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.REFUSEE);
        assertThat(journalDe(d)).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("REFUS");
            assertThat(e.apres()).contains("REFUSEE");
            assertThat(e.detail()).contains("Ancienneté trop faible");
        });
    }

    @Test
    void refus_sans_motif_ni_journal_ni_evenement() {
        Demande d = enAnalyse();

        assertThatThrownBy(() -> service.refuser(d.id(), "analyste", " "))
                .isInstanceOf(MotifRefusInvalideException.class);

        assertThat(journalDe(d)).isEmpty();
        assertThat(evenements).isEmpty();
    }

    @Test
    void demander_un_justificatif_laisse_le_dossier_en_analyse_et_est_journalise() {
        Demande d = enAnalyse();

        service.demanderJustificatif(d.id(), "analyste", TypeDocument.BULLETIN_PAIE, " bulletin d'octobre lisible ");

        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.EN_ANALYSE);
        assertThat(journalDe(d)).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("DEMANDE_JUSTIFICATIF");
            assertThat(e.avant()).isEmpty();
            assertThat(e.apres()).isEmpty();
            assertThat(e.detail()).contains("Bulletin de paie — bulletin d'octobre lisible");
        });
        assertThat(evenements).containsExactly(
                new JustificatifDemande(d.id(), d.clientId(), TypeDocument.BULLETIN_PAIE, MAINTENANT));
    }

    @Test
    void precision_trop_longue_refusee() {
        Demande d = enAnalyse();

        assertThatThrownBy(() -> service.demanderJustificatif(d.id(), "analyste", TypeDocument.RIB,
                "x".repeat(DecisionService.LONGUEUR_PRECISION_MAX + 1)))
                .isInstanceOf(PrecisionJustificatifTropLongueException.class);
        assertThat(journalDe(d)).isEmpty();
    }

    @Test
    void une_seconde_decision_est_interdite_par_la_machine_a_etats() {
        Demande d = enAnalyse();
        service.accepter(d.id(), "analyste");

        assertThatThrownBy(() -> service.refuser(d.id(), "autre", "Motif"))
                .isInstanceOf(TransitionInterditeException.class);
        assertThatThrownBy(() -> service.demanderJustificatif(d.id(), "autre", TypeDocument.RIB, null))
                .isInstanceOf(DecisionImpossibleException.class);

        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.ACCEPTEE);
        assertThat(journalDe(d)).hasSize(1);
    }

    @Test
    void dossier_inconnu() {
        assertThatThrownBy(() -> service.accepter(UUID.randomUUID(), "analyste"))
                .isInstanceOf(DossierIntrouvableException.class);
    }
}
