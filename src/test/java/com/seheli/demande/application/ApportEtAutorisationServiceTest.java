package com.seheli.demande.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.audit.domain.TypeActeur;
import com.seheli.contrat.domain.Contrat;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandesFixtures;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.paiement.domain.ReferenceApport;

class ApportEtAutorisationServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-14T10:00:00Z");

    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(new JournalEnMemoire());
    private final List<Object> evenements = new ArrayList<>();
    private final PrealablesDeTest prealables = new PrealablesDeTest(clock);
    private final ApportEtAutorisationService service = new ApportEtAutorisationService(demandes,
            prealables.service, journal, evenements::add, clock);

    private Demande acceptee() {
        Demande d = DemandesFixtures.decidee(UUID.randomUUID(), DemandesFixtures.termes(), StatutDossier.ACCEPTEE,
                "analyste");
        demandes.enregistrer(d);
        return d;
    }

    private StatutDossier statut(Demande d) {
        return demandes.parId(d.id()).orElseThrow().statut();
    }

    @Test
    void apport_encaisse_et_autorisation_acceptee_le_dossier_passe_a_apport_et_autorisation() {
        Demande d = acceptee();
        Contrat c = prealables.contrat(d);
        prealables.payerApport(c);
        prealables.accepterAutorisation(c);

        assertThat(service.constaterSiReunis(d.id())).isTrue();

        assertThat(statut(d)).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
        assertThat(journal.historique(JournalDossier.objet(d.id()))).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("APPORT_ET_AUTORISATION_RECUS");
            assertThat(e.avant()).contains("ACCEPTEE");
            assertThat(e.apres()).contains("APPORT_ET_AUTORISATION");
            assertThat(e.acteur().type()).isEqualTo(TypeActeur.SYSTEME);
        });
        assertThat(evenements).containsExactly(new StatutDossierChange(d.id(), d.clientId(), StatutDossier.ACCEPTEE,
                StatutDossier.APPORT_ET_AUTORISATION, MAINTENANT));

        assertThat(service.constaterSiReunis(d.id())).as("idempotent").isFalse();
        assertThat(evenements).hasSize(1);
    }

    @Test
    void sans_l_autorisation_acceptee_le_dossier_reste_accepte() {
        Demande d = acceptee();
        Contrat c = prealables.contrat(d);
        prealables.payerApport(c);

        assertThat(service.constaterSiReunis(d.id())).isFalse();
        assertThat(statut(d)).isEqualTo(StatutDossier.ACCEPTEE);
        assertThat(evenements).isEmpty();
    }

    @Test
    void sans_l_apport_le_dossier_reste_accepte() {
        Demande d = acceptee();
        Contrat c = prealables.contrat(d);
        prealables.accepterAutorisation(c);

        assertThat(service.constaterSiReunis(d.id())).isFalse();
        assertThat(statut(d)).isEqualTo(StatutDossier.ACCEPTEE);
    }

    @Test
    void un_apport_partiel_ne_suffit_pas() {
        Demande d = acceptee();
        Contrat c = prealables.contrat(d);
        prealables.accepterAutorisation(c);
        String reference = ReferenceApport.pourContrat(c.numero()).valeur();
        prealables.payer(c, reference, "8000");

        assertThat(service.constaterSiReunis(d.id())).isFalse();
    }

    @Test
    void une_autorisation_revoquee_ne_compte_pas() {
        Demande d = acceptee();
        Contrat c = prealables.contrat(d);
        prealables.payerApport(c);
        prealables.accepterAutorisation(c);
        prealables.revoquerAutorisation(c);

        assertThat(service.constaterSiReunis(d.id())).isFalse();
        assertThat(statut(d)).isEqualTo(StatutDossier.ACCEPTEE);
    }

    @Test
    void sans_contrat_ou_dossier_inconnu_rien_ne_change() {
        Demande d = acceptee();

        assertThat(service.constaterSiReunis(d.id())).isFalse();
        assertThat(service.constaterSiReunis(UUID.randomUUID())).isFalse();
    }
}
