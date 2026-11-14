package com.seheli.demande.domain;

import java.util.Objects;

/** Points obtenus sur un critère du score, avec la raison lisible par l'analyste. */
public record ResultatCritere(CritereScore critere, int points, String raison) {

    public ResultatCritere {
        Objects.requireNonNull(critere, "critere");
        Objects.requireNonNull(raison, "raison");
        if (points < 0 || points > critere.pointsMax()) {
            throw new IllegalArgumentException("Points hors barème pour " + critere + " : " + points
                    + " (maximum " + critere.pointsMax() + ")");
        }
    }

    public int pointsMax() {
        return critere.pointsMax();
    }
}
