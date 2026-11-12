package com.cic.demonstration.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.cic.contrat.domain.EcheanceAPrelever;
import com.cic.demande.application.ApportEtAutorisationService;
import com.cic.demande.application.ExpirationDossiersService;
import com.cic.demonstration.domain.Simulation;
import com.cic.recouvrement.application.RappelPrelevementService;
import com.cic.recouvrement.application.RetardEcheanceService;
import com.cic.recouvrement.domain.RelanceDue;

/**
 * Les traitements planifiés de l'application, rejoués chaque jour simulé aux heures de leurs jobs : annulation
 * des dossiers sans apport ou autorisation après le délai, détection des retards et relances après échec
 * (8 h), rappels avant prélèvement (9 h). Mêmes services, même enchaînement que les jobs.
 */
@Component
@Profile("demo")
class TachesPlanifieesSimulees {

    private static final LocalTime EXPIRATION = LocalTime.of(7, 0);
    private static final LocalTime RETARDS_ET_RELANCES = LocalTime.of(8, 0);
    private static final LocalTime RAPPELS = LocalTime.of(9, 0);

    private final ExpirationDossiersService expiration;
    private final ApportEtAutorisationService apportEtAutorisation;
    private final RetardEcheanceService retards;
    private final RappelPrelevementService rappels;

    TachesPlanifieesSimulees(ExpirationDossiersService expiration, ApportEtAutorisationService apportEtAutorisation,
            RetardEcheanceService retards, RappelPrelevementService rappels) {
        this.expiration = expiration;
        this.apportEtAutorisation = apportEtAutorisation;
        this.retards = retards;
        this.rappels = rappels;
    }

    /** Planifie les traitements de chaque jour, de {@code premierJour} à aujourd'hui inclus. */
    void planifier(Simulation simulation, LocalDate premierJour) {
        for (LocalDate jour = premierJour; !jour.isAfter(simulation.aujourdhui()); jour = jour.plusDays(1)) {
            simulation.planifier(jour, EXPIRATION, this::annulerDossiersADelaiDepasse);
            simulation.planifier(jour, RETARDS_ET_RELANCES, this::retardsEtRelances);
            simulation.planifier(jour, RAPPELS, this::rappelsAvantPrelevement);
        }
    }

    private void annulerDossiersADelaiDepasse() {
        for (UUID id : expiration.dossiersADelaiDepasse()) {
            if (!apportEtAutorisation.constaterSiReunis(id)) {
                expiration.annulerSiDelaiDepasse(id);
            }
        }
    }

    private void retardsEtRelances() {
        for (EcheanceAPrelever e : retards.echeancesImpayees()) {
            retards.constaterRetard(e);
        }
        for (EcheanceAPrelever e : retards.echeancesEnRetard()) {
            retards.rattraperDossier(e);
        }
        for (RelanceDue due : retards.relancesDues()) {
            retards.relancer(due);
        }
    }

    private void rappelsAvantPrelevement() {
        for (EcheanceAPrelever e : rappels.echeancesARappeler()) {
            rappels.rappeler(e);
        }
    }
}
