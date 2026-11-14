package com.seheli.demande.domain;

import java.util.Objects;

import com.seheli.commun.domain.Fraction;

/**
 * Paramètres des conditions éliminatoires, lus dans la table {@code parametre} :
 * {@code age.minimum.ans} et {@code endettement.max.fraction}.
 */
public record ReglesEligibilite(int ageMinimumAns, Fraction endettementMax) {

    public ReglesEligibilite {
        if (ageMinimumAns < 0) {
            throw new IllegalArgumentException("Âge minimum invalide : " + ageMinimumAns);
        }
        Objects.requireNonNull(endettementMax, "endettementMax");
    }
}
