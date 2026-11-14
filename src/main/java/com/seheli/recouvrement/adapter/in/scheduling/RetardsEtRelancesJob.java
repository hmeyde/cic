package com.seheli.recouvrement.adapter.in.scheduling;

import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.recouvrement.application.RetardEcheanceService;
import com.seheli.recouvrement.domain.RelanceDue;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Job planifié des retards : échéances impayées à leur date passées EN_RETARD (et leur dossier),
 * puis relances à J+3 et J+7. Fréquence {@code seheli.jobs.retards-relances.cron} (tous les jours à 8 h,
 * heure de Nouakchott, par défaut) ; verrou ShedLock pour qu'un seul nœud l'exécute. Chaque étape
 * est idempotente ; une échéance en erreur est journalisée et reprise au passage suivant.
 */
@Component
class RetardsEtRelancesJob {

    private static final Logger LOG = LoggerFactory.getLogger(RetardsEtRelancesJob.class);

    private final RetardEcheanceService retards;

    RetardsEtRelancesJob(RetardEcheanceService retards) {
        this.retards = retards;
    }

    @Scheduled(cron = "${seheli.jobs.retards-relances.cron:0 0 8 * * *}", zone = "Africa/Nouakchott")
    @SchedulerLock(name = "retards-relances", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    void executer() {
        detecterRetards();
        envoyerRelances();
    }

    /** @return nombre d'échéances passées EN_RETARD */
    int detecterRetards() {
        List<EcheanceAPrelever> impayees = retards.echeancesImpayees();
        int nouveaux = traiter(impayees, retards::constaterRetard, "Constat du retard");
        int dossiers = traiter(retards.echeancesEnRetard(), retards::rattraperDossier, "Passage EN_RETARD du dossier");
        if (nouveaux > 0 || dossiers > 0) {
            LOG.info("Retards : {} échéance(s) passée(s) EN_RETARD, {} dossier(s) rattrapé(s)", nouveaux, dossiers);
        }
        return nouveaux;
    }

    /** @return nombre de relances envoyées */
    int envoyerRelances() {
        List<RelanceDue> dues = retards.relancesDues();
        int envoyees = 0;
        for (RelanceDue due : dues) {
            try {
                if (retards.relancer(due)) {
                    envoyees++;
                }
            } catch (RuntimeException ex) {
                LOG.error("Relance {} de l'échéance {} du contrat {} impossible, reprise au prochain passage",
                        due.type(), due.echeance().echeance().rang(), due.echeance().numeroContrat(), ex);
            }
        }
        if (!dues.isEmpty()) {
            LOG.info("Relances après échec : {} envoyée(s) sur {} due(s)", envoyees, dues.size());
        }
        return envoyees;
    }

    private static int traiter(List<EcheanceAPrelever> echeances, Predicate<EcheanceAPrelever> action,
            String libelle) {
        int faits = 0;
        for (EcheanceAPrelever e : echeances) {
            try {
                if (action.test(e)) {
                    faits++;
                }
            } catch (RuntimeException ex) {
                LOG.error("{} de l'échéance {} du contrat {} impossible, reprise au prochain passage", libelle,
                        e.echeance().rang(), e.numeroContrat(), ex);
            }
        }
        return faits;
    }
}
