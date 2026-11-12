package com.cic.demande.adapter.in.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.contrat.domain.Contrat;
import com.cic.demande.application.ApportEtAutorisationService;
import com.cic.demande.application.DemandesEnMemoire;
import com.cic.demande.application.ExpirationDossiersService;
import com.cic.demande.application.PrealablesDeTest;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandesFixtures;
import com.cic.demande.domain.StatutDossier;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

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
