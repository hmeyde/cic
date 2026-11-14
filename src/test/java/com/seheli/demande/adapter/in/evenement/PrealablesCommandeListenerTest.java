package com.seheli.demande.adapter.in.evenement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.contrat.AutorisationPrelevementStatutChange;
import com.seheli.contrat.domain.AutorisationPrelevement;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.StatutAutorisation;
import com.seheli.demande.application.ApportEtAutorisationService;
import com.seheli.demande.application.DemandesEnMemoire;
import com.seheli.demande.application.PrealablesDeTest;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandesFixtures;
import com.seheli.demande.domain.StatutDossier;

class PrealablesCommandeListenerTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-14T10:00:00Z");

    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final PrealablesDeTest prealables = new PrealablesDeTest(clock);
    private final PrealablesCommandeListener listener = new PrealablesCommandeListener(
            new ApportEtAutorisationService(demandes, prealables.service,
                    new JournalAuditService(new JournalEnMemoire()), e -> { }, clock));

    @Test
    void le_second_des_deux_evenements_fait_passer_le_dossier() {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.ACCEPTEE);
        demandes.enregistrer(d);
        Contrat c = prealables.contrat(d);
        AutorisationPrelevement a = prealables.autorisations.parContrat(c.id()).orElseThrow();

        prealables.accepterAutorisation(c);
        listener.autorisationChangee(new AutorisationPrelevementStatutChange(a.id(), d.id(), d.clientId(),
                StatutAutorisation.TRANSMISE, StatutAutorisation.ACCEPTEE, MAINTENANT));
        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.ACCEPTEE);

        prealables.payerApport(c);
        listener.paiementRapproche(new com.seheli.paiement.PaiementRapproche(UUID.randomUUID(), d.id(), c.id(),
                c.numero(), d.clientId(), com.seheli.paiement.ObjetPaiement.APPORT, null, c.apport(), c.apport(),
                MAINTENANT.atZone(clock.getZone()).toLocalDate(), MAINTENANT));
        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
    }

    @Test
    void une_autorisation_seulement_signee_ne_declenche_rien() {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.ACCEPTEE);
        demandes.enregistrer(d);
        Contrat c = prealables.contrat(d);
        prealables.payerApport(c);

        listener.autorisationChangee(new AutorisationPrelevementStatutChange(UUID.randomUUID(), d.id(), d.clientId(),
                StatutAutorisation.A_SIGNER, StatutAutorisation.SIGNEE, MAINTENANT));

        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.ACCEPTEE);
    }
}
