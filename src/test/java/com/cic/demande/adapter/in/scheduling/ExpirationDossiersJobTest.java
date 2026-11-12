package com.cic.demande.adapter.in.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.demande.application.DemandesEnMemoire;
import com.cic.demande.application.ExpirationDossiersService;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandesFixtures;
import com.cic.demande.domain.StatutDossier;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class ExpirationDossiersJobTest {

    @Test
    void le_job_annule_les_dossiers_expires_et_laisse_les_autres() {
        DemandesEnMemoire demandes = new DemandesEnMemoire();
        Demande expire = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.ACCEPTEE);
        Demande complet = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.APPORT_ET_AUTORISATION);
        demandes.enregistrer(expire);
        demandes.enregistrer(complet);
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        Clock septJoursApres = Clock.fixed(DemandesFixtures.LE.plus(Duration.ofDays(7)), ZoneOffset.UTC);
        ExpirationDossiersJob job = new ExpirationDossiersJob(new ExpirationDossiersService(demandes, parametres,
                new JournalAuditService(new JournalEnMemoire()), e -> { }, septJoursApres));

        assertThat(job.annulerDossiersADelaiDepasse()).isEqualTo(1);
        assertThat(demandes.parId(expire.id()).orElseThrow().statut()).isEqualTo(StatutDossier.ANNULEE);
        assertThat(demandes.parId(complet.id()).orElseThrow().statut())
                .isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);

        assertThat(job.annulerDossiersADelaiDepasse()).isZero();
    }
}
