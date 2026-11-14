package com.seheli.recouvrement.adapter.in.scheduling;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.recouvrement.application.RappelPrelevementService;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Job planifié : message au client avant chaque prélèvement. Fréquence
 * {@code seheli.jobs.rappel-prelevement.cron} (tous les jours à 9 h, heure de Nouakchott, par défaut) ;
 * verrou ShedLock pour qu'un seul nœud l'exécute. Le service est idempotent : relancer le job
 * n'envoie pas deux fois le même rappel. Une échéance en erreur est journalisée et n'empêche pas de
 * traiter les suivantes ; elle sera reprise au passage suivant.
 */
@Component
class RappelPrelevementJob {

    private static final Logger LOG = LoggerFactory.getLogger(RappelPrelevementJob.class);

    private final RappelPrelevementService rappels;

    RappelPrelevementJob(RappelPrelevementService rappels) {
        this.rappels = rappels;
    }

    @Scheduled(cron = "${seheli.jobs.rappel-prelevement.cron:0 0 9 * * *}", zone = "Africa/Nouakchott")
    @SchedulerLock(name = "rappel-prelevement", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    void executer() {
        envoyerRappels();
    }

    /** @return nombre de rappels envoyés */
    int envoyerRappels() {
        List<EcheanceAPrelever> echeances = rappels.echeancesARappeler();
        int envoyes = 0;
        for (EcheanceAPrelever e : echeances) {
            try {
                if (rappels.rappeler(e)) {
                    envoyes++;
                }
            } catch (RuntimeException ex) {
                LOG.error("Rappel de l'échéance {} du contrat {} impossible, reprise au prochain passage",
                        e.echeance().rang(), e.numeroContrat(), ex);
            }
        }
        if (!echeances.isEmpty()) {
            LOG.info("Rappels avant prélèvement : {} envoyé(s) sur {} échéance(s) à venir", envoyes, echeances.size());
        }
        return envoyes;
    }
}
