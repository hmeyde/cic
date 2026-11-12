package com.cic.demande.adapter.in.scheduling;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cic.demande.application.ExpirationDossiersService;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Job planifié : annule les dossiers ACCEPTEE dont l'apport ou l'autorisation de prélèvement manque
 * après le délai. Fréquence {@code cic.jobs.expiration-dossiers.cron} (toutes les 15 minutes par
 * défaut) ; verrou ShedLock pour qu'un seul nœud l'exécute à la fois. Un dossier en erreur est
 * journalisé et n'empêche pas de traiter les suivants ; il sera repris au passage suivant.
 */
@Component
class ExpirationDossiersJob {

    private static final Logger LOG = LoggerFactory.getLogger(ExpirationDossiersJob.class);

    private final ExpirationDossiersService expiration;

    ExpirationDossiersJob(ExpirationDossiersService expiration) {
        this.expiration = expiration;
    }

    @Scheduled(cron = "${cic.jobs.expiration-dossiers.cron:0 */15 * * * *}", zone = "Africa/Nouakchott")
    @SchedulerLock(name = "expiration-dossiers", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    void executer() {
        annulerDossiersADelaiDepasse();
    }

    /** @return nombre de dossiers annulés */
    int annulerDossiersADelaiDepasse() {
        List<UUID> candidats = expiration.dossiersADelaiDepasse();
        int annules = 0;
        for (UUID id : candidats) {
            try {
                if (expiration.annulerSiDelaiDepasse(id)) {
                    annules++;
                }
            } catch (RuntimeException e) {
                LOG.error("Annulation du dossier {} impossible, reprise au prochain passage", id, e);
            }
        }
        if (!candidats.isEmpty()) {
            LOG.info("Expiration des dossiers : {} annulé(s) sur {} candidat(s)", annules, candidats.size());
        }
        return annules;
    }
}
