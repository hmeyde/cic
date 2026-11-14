package com.seheli.demande.adapter.in.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.contrat.domain.Contrat;
import com.seheli.demande.application.ApportEtAutorisationService;
import com.seheli.demande.application.DemandesEnMemoire;
import com.seheli.demande.application.ExpirationDossiersService;
import com.seheli.demande.application.PrealablesDeTest;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandesFixtures;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class ExpirationDossiersJobTest {

    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final Clock septJoursApres = Clock.fixed(DemandesFixtures.LE.plus(Duration.ofDays(7)), ZoneOffset.UTC);
    private final PrealablesDeTest prealables = new PrealablesDeTest(septJoursApres);
    private final ExpirationDossiersJob job;

    ExpirationDossiersJobTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        JournalAuditService journal = new JournalAuditService(new JournalEnMemoire());
        job = new ExpirationDossiersJob(
                new ExpirationDossiersService(demandes, parametres, journal, e -> { }, septJoursApres),
                new ApportEtAutorisationService(demandes, prealables.service, journal, e -> { }, septJoursApres));
    }

    @Test
    void le_job_annule_les_dossiers_expires_et_laisse_les_autres() {
        Demande expire = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.ACCEPTEE);
        Demande complet = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.APPORT_ET_AUTORISATION);
        demandes.enregistrer(expire);
        demandes.enregistrer(complet);

        assertThat(job.annulerDossiersADelaiDepasse()).isEqualTo(1);
        assertThat(demandes.parId(expire.id()).orElseThrow().statut()).isEqualTo(StatutDossier.ANNULEE);
        assertThat(demandes.parId(complet.id()).orElseThrow().statut())
                .isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);

        assertThat(job.annulerDossiersADelaiDepasse()).isZero();
    }

    @Test
    void un_dossier_qui_a_recu_l_apport_et_l_autorisation_n_est_pas_annule() {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.ACCEPTEE);
        demandes.enregistrer(d);
        Contrat c = prealables.contrat(d);
        prealables.payerApport(c);
        prealables.accepterAutorisation(c);

        assertThat(job.annulerDossiersADelaiDepasse()).isZero();
        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
    }
}
