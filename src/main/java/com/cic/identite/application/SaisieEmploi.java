package com.cic.identite.application;

import java.time.LocalDate;

import com.cic.commun.domain.Montant;
import com.cic.identite.domain.ModeRemboursement;
import com.cic.identite.domain.TypeContrat;

/** Emploi déclaré par le client ; {@code dateFin} est nulle sauf pour un CDD. */
public record SaisieEmploi(
        String employeur,
        TypeContrat typeContrat,
        LocalDate dateDebut,
        LocalDate dateFin,
        Montant salaireNet,
        int jourPaie,
        boolean salaireVerseSurCompte,
        boolean salaireDomicilieBmci,
        ModeRemboursement modeRemboursement) {
}
