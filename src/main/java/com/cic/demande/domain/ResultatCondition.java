package com.cic.demande.domain;

import java.util.Objects;

/** Résultat d'une condition éliminatoire, avec la raison lisible par l'analyste. */
public record ResultatCondition(ConditionEliminatoire condition, boolean satisfaite, String raison) {

    public ResultatCondition {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(raison, "raison");
    }

    static ResultatCondition satisfaite(ConditionEliminatoire condition, String raison) {
        return new ResultatCondition(condition, true, raison);
    }

    static ResultatCondition nonSatisfaite(ConditionEliminatoire condition, String raison) {
        return new ResultatCondition(condition, false, raison);
    }
}
