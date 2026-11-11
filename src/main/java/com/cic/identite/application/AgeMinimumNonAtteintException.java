package com.cic.identite.application;

import com.cic.commun.domain.ErreurMetier;

/** Le client n'a pas l'âge minimum ({@code age.minimum.ans}) pour ouvrir un compte. */
public class AgeMinimumNonAtteintException extends ErreurMetier {

    private final int ageMinimum;

    public AgeMinimumNonAtteintException(int ageMinimum) {
        super("client.compte.age.minimum", "Âge minimum non atteint");
        this.ageMinimum = ageMinimum;
    }

    public int ageMinimum() {
        return ageMinimum;
    }
}
