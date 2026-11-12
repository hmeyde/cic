package com.cic.comptabilite.adapter.out.persistence;

import java.math.BigDecimal;

import com.cic.comptabilite.domain.Sens;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Ligne d'une écriture : numéro de compte, sens et montant. */
@Embeddable
class LigneEcritureEmbeddable {

    @Column(name = "compte", nullable = false, updatable = false, length = 10)
    private String compte;

    @Enumerated(EnumType.STRING)
    @Column(name = "sens", nullable = false, updatable = false, length = 6)
    private Sens sens;

    @Column(name = "montant", nullable = false, updatable = false, precision = 14, scale = 0)
    private BigDecimal montant;

    protected LigneEcritureEmbeddable() {
    }

    String getCompte() {
        return compte;
    }

    Sens getSens() {
        return sens;
    }

    BigDecimal getMontant() {
        return montant;
    }
}
