package com.seheli.demande.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.seheli.demande.domain.ConditionEliminatoire;

@Embeddable
class ScoreConditionEmbeddable {

    @Column(name = "rang", nullable = false)
    private int rang;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_eliminatoire", nullable = false, length = 30)
    private ConditionEliminatoire condition;

    @Column(name = "satisfaite", nullable = false)
    private boolean satisfaite;

    @Column(name = "raison", nullable = false, length = 500)
    private String raison;

    protected ScoreConditionEmbeddable() {
    }

    ScoreConditionEmbeddable(int rang, ConditionEliminatoire condition, boolean satisfaite, String raison) {
        this.rang = rang;
        this.condition = condition;
        this.satisfaite = satisfaite;
        this.raison = raison;
    }

    ConditionEliminatoire getCondition() {
        return condition;
    }

    boolean isSatisfaite() {
        return satisfaite;
    }

    String getRaison() {
        return raison;
    }
}
