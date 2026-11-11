package com.cic.catalogue.application;

import java.math.BigDecimal;

import com.cic.commun.domain.ErreurMetier;

/** La remise négociée sort des bornes {@code remise.fournisseur.min.pct} – {@code remise.fournisseur.max.pct}. */
public class RemiseHorsBornesException extends ErreurMetier {

    public static final String CODE = "bo.fournisseur.remise.hors.bornes";

    private final BigDecimal minPct;
    private final BigDecimal maxPct;

    public RemiseHorsBornesException(BigDecimal remisePct, BigDecimal minPct, BigDecimal maxPct) {
        super(CODE, "Remise fournisseur " + remisePct.toPlainString() + " % hors bornes ["
                + minPct.toPlainString() + " ; " + maxPct.toPlainString() + "] %");
        this.minPct = minPct;
        this.maxPct = maxPct;
    }

    public BigDecimal minPct() {
        return minPct;
    }

    public BigDecimal maxPct() {
        return maxPct;
    }
}
