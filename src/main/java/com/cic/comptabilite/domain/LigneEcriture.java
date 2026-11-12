package com.cic.comptabilite.domain;

import java.util.Objects;

import com.cic.commun.domain.Montant;

/** Ligne d'une écriture : un compte, débité ou crédité d'un montant strictement positif. */
public record LigneEcriture(Compte compte, Sens sens, Montant montant) {

    public LigneEcriture {
        Objects.requireNonNull(compte, "compte");
        Objects.requireNonNull(sens, "sens");
        Objects.requireNonNull(montant, "montant");
        if (montant.estZero()) {
            throw new IllegalArgumentException("Une ligne d'écriture a un montant strictement positif");
        }
    }

    public static LigneEcriture debit(Compte compte, Montant montant) {
        return new LigneEcriture(compte, Sens.DEBIT, montant);
    }

    public static LigneEcriture credit(Compte compte, Montant montant) {
        return new LigneEcriture(compte, Sens.CREDIT, montant);
    }

    /** Même compte et même montant, dans l'autre sens. */
    public LigneEcriture inverse() {
        return new LigneEcriture(compte, sens.inverse(), montant);
    }
}
