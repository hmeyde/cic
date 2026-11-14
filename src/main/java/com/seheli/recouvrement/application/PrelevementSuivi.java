package com.seheli.recouvrement.application;

import java.util.Objects;

import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.demande.domain.StatutDossier;

/** Prélèvement d'une échéance tel que le suit le recouvrement, avec le statut de son dossier. */
public record PrelevementSuivi(EcheanceAPrelever echeance, StatutDossier statutDossier) {

    public PrelevementSuivi {
        Objects.requireNonNull(echeance, "echeance");
        Objects.requireNonNull(statutDossier, "statutDossier");
    }
}
