package com.cic.comptabilite.domain;

/** Sens d'une ligne d'écriture. */
public enum Sens {
    DEBIT,
    CREDIT;

    public Sens inverse() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
