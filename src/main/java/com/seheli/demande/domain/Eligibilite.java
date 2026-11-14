package com.seheli.demande.domain;

import java.util.List;
import java.util.Objects;

/** Résultat de toutes les conditions éliminatoires, dans l'ordre de la grille. */
public record Eligibilite(List<ResultatCondition> resultats) {

    public Eligibilite {
        Objects.requireNonNull(resultats, "resultats");
        resultats = List.copyOf(resultats);
    }

    /** Vrai si toutes les conditions sont satisfaites. */
    public boolean estEligible() {
        return resultats.stream().allMatch(ResultatCondition::satisfaite);
    }

    public List<ResultatCondition> nonSatisfaites() {
        return resultats.stream().filter(r -> !r.satisfaite()).toList();
    }

    public ResultatCondition resultat(ConditionEliminatoire condition) {
        return resultats.stream().filter(r -> r.condition() == condition).findFirst().orElseThrow();
    }
}
