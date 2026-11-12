package com.cic.demande.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.cic.demande.domain.CritereScore;

/** Les points maximaux sont recopiés pour que le barème appliqué reste lisible en base. */
@Embeddable
class ScoreCritereEmbeddable {

    @Column(name = "rang", nullable = false)
    private int rang;

    @Enumerated(EnumType.STRING)
    @Column(name = "critere", nullable = false, length = 30)
    private CritereScore critere;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "points_max", nullable = false)
    private int pointsMax;

    @Column(name = "raison", nullable = false, length = 500)
    private String raison;

    protected ScoreCritereEmbeddable() {
    }

    ScoreCritereEmbeddable(int rang, CritereScore critere, int points, int pointsMax, String raison) {
        this.rang = rang;
        this.critere = critere;
        this.points = points;
        this.pointsMax = pointsMax;
        this.raison = raison;
    }

    CritereScore getCritere() {
        return critere;
    }

    int getPoints() {
        return points;
    }

    String getRaison() {
        return raison;
    }
}
