package com.seheli.catalogue.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fournisseur")
class FournisseurEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "remise_negociee_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal remiseNegocieePct;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    protected FournisseurEntity() {
    }

    FournisseurEntity(UUID id, String nom, BigDecimal remiseNegocieePct, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.remiseNegocieePct = remiseNegocieePct;
        this.actif = actif;
    }

    UUID getId() {
        return id;
    }

    String getNom() {
        return nom;
    }

    BigDecimal getRemiseNegocieePct() {
        return remiseNegocieePct;
    }

    boolean isActif() {
        return actif;
    }
}
