package com.seheli.demande.domain;

import java.util.Objects;

/** Transition permise d'un dossier, à consigner dans le journal d'audit. */
public record ChangementStatut(StatutDossier depuis, StatutDossier vers) {

    public ChangementStatut {
        Objects.requireNonNull(depuis, "depuis");
        Objects.requireNonNull(vers, "vers");
        if (!depuis.peutPasserA(vers)) {
            throw new TransitionInterditeException(depuis, vers);
        }
    }
}
