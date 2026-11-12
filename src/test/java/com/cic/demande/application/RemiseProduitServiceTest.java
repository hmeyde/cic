package com.cic.demande.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.audit.domain.TypeActeur;
import com.cic.contrat.domain.Contrat;
import com.cic.demande.StatutDossierChange;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandesFixtures;
import com.cic.demande.domain.StatutDossier;
import com.cic.demande.domain.TransitionInterditeException;

class RemiseProduitServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-20T10:00:00Z");

    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(new JournalEnMemoire());
    private final List<Object> evenements = new ArrayList<>();
    private final PrealablesDeTest modules = new PrealablesDeTest(clock);
    private final RemiseProduitService service = new RemiseProduitService(demandes, modules.contratService, journal,
            evenements::add, clock);
    private final UUID clientId = UUID.randomUUID();

    private Demande dossier(StatutDossier statut) {
        Demande d = DemandesFixtures.decidee(clientId, DemandesFixtures.termes(), statut, "analyste");
        demandes.enregistrer(d);
        modules.contrat(d);
        return d;
    }

    private StatutDossier statut(Demande d) {
        return demandes.parId(d.id()).orElseThrow().statut();
    }

    @Test
    void la_confirmation_de_remise_fait_demarrer_le_remboursement() {
        Demande d = dossier(StatutDossier.COMMANDEE);
        Contrat contrat = modules.contratService.parDemande(d.id()).orElseThrow();

        service.confirmer(clientId, true);

        assertThat(statut(d)).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
        assertThat(demandes.verrous()).isEqualTo(1);
        assertThat(journal.historique(JournalDossier.objet(d.id()))).satisfiesExactlyInAnyOrder(
                e -> {
                    assertThat(e.action()).isEqualTo("CONFIRMATION_REMISE");
                    assertThat(e.avant()).contains("COMMANDEE");
                    assertThat(e.apres()).contains("LIVREE");
                    assertThat(e.acteur().type()).isEqualTo(TypeActeur.CLIENT);
                },
                e -> {
                    assertThat(e.action()).isEqualTo("DEBUT_REMBOURSEMENT");
                    assertThat(e.avant()).contains("LIVREE");
                    assertThat(e.apres()).contains("EN_REMBOURSEMENT");
                    assertThat(e.acteur().type()).isEqualTo(TypeActeur.SYSTEME);
                    assertThat(e.detail()).hasValueSatisfying(t -> assertThat(t).contains(contrat.numero(), "6 échéances"));
                });
        assertThat(evenements).containsExactly(
                new StatutDossierChange(d.id(), clientId, StatutDossier.COMMANDEE, StatutDossier.LIVREE, MAINTENANT),
                new StatutDossierChange(d.id(), clientId, StatutDossier.LIVREE, StatutDossier.EN_REMBOURSEMENT,
                        MAINTENANT));
        assertThat(modules.contratService.parDemande(d.id()).orElseThrow().echeances())
                .as("échéancier fixé à la signature, inchangé").isEqualTo(contrat.echeances());
    }

    @Test
    void sans_la_case_cochee_rien_ne_change() {
        Demande d = dossier(StatutDossier.COMMANDEE);

        assertThatThrownBy(() -> service.confirmer(clientId, false)).isInstanceOf(RemiseNonConfirmeeException.class);

        assertThat(statut(d)).isEqualTo(StatutDossier.COMMANDEE);
        assertThat(evenements).isEmpty();
    }

    @Test
    void un_produit_pas_encore_commande_ne_peut_pas_etre_remis() {
        Demande d = dossier(StatutDossier.APPORT_ET_AUTORISATION);

        assertThatThrownBy(() -> service.confirmer(clientId, true)).isInstanceOf(TransitionInterditeException.class);

        assertThat(statut(d)).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
        assertThat(journal.historique(JournalDossier.objet(d.id()))).isEmpty();
        assertThat(evenements).isEmpty();
    }

    @Test
    void une_seconde_confirmation_est_refusee() {
        Demande d = dossier(StatutDossier.COMMANDEE);
        service.confirmer(clientId, true);

        assertThatThrownBy(() -> service.confirmer(clientId, true)).isInstanceOf(TransitionInterditeException.class);

        assertThat(statut(d)).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
        assertThat(evenements).hasSize(2);
    }

    @Test
    void seul_le_dossier_du_client_est_concerne() {
        Demande d = dossier(StatutDossier.COMMANDEE);

        assertThatThrownBy(() -> service.confirmer(UUID.randomUUID(), true))
                .isInstanceOf(DossierIntrouvableException.class);

        assertThat(statut(d)).isEqualTo(StatutDossier.COMMANDEE);
    }
}
