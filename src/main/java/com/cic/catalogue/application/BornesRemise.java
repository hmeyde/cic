package com.cic.catalogue.application;

import java.math.BigDecimal;
import java.util.Objects;

/** Bornes de la remise fournisseur en vigueur, lues dans la table {@code parametre}. */
public record BornesRemise(BigDecimal minPct, BigDecimal maxPct) {

    public BornesRemise {
        Objects.requireNonNull(minPct, "minPct");
        Objects.requireNonNull(maxPct, "maxPct");
    }

    public boolean contient(BigDecimal remisePct) {
        return remisePct.compareTo(minPct) >= 0 && remisePct.compareTo(maxPct) <= 0;
    }
}
