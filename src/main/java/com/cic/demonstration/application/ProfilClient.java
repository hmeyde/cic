package com.cic.demonstration.application;

import java.time.LocalDate;

import com.cic.identite.domain.ModeRemboursement;
import com.cic.identite.domain.TypeContrat;

/** Identité et emploi d'un client fictif ; {@code dateFin} est nulle sauf pour un CDD. */
record ProfilClient(String telephone, String nni, String prenom, String nom, LocalDate dateNaissance,
        String employeur, TypeContrat typeContrat, LocalDate dateDebut, LocalDate dateFin, long salaireNet,
        int jourPaie, ModeRemboursement modeRemboursement) {

    @Override
    public String toString() {
        return "ProfilClient[" + typeContrat + "]";
    }
}
