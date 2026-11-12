package com.cic.contrat.domain;

import java.util.Objects;

/** Transition appliquée à une autorisation de prélèvement. */
public record ChangementStatutAutorisation(StatutAutorisation depuis, StatutAutorisation vers) {

    public ChangementStatutAutorisation {
        Objects.requireNonNull(depuis, "depuis");
        Objects.requireNonNull(vers, "vers");
    }
}
