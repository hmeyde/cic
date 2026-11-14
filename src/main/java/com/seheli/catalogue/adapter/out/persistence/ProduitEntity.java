package com.seheli.catalogue.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.seheli.catalogue.domain.CategorieProduit;

/** Le fournisseur est référencé par identifiant (clé étrangère en base, sans relation JPA). */
@Entity
@Table(name = "produit")
class ProduitEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "reference", nullable = false, length = 50, unique = true)
    private String reference;

    @Column(name = "designation", nullable = false, length = 200)
    private String designation;

    @Column(name = "marque", nullable = false, length = 100)
    private String marque;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 30)
    private CategorieProduit categorie;

    @Column(name = "fournisseur_id", nullable = false)
    private UUID fournisseurId;

    @Column(name = "prix_public", nullable = false, precision = 12, scale = 0)
    private BigDecimal prixPublic;

    @Column(name = "disponible", nullable = false)
    private boolean disponible;

    protected ProduitEntity() {
    }

    ProduitEntity(UUID id, String reference, String designation, String marque, CategorieProduit categorie,
            UUID fournisseurId, BigDecimal prixPublic, boolean disponible) {
        this.id = id;
        this.reference = reference;
        this.designation = designation;
        this.marque = marque;
        this.categorie = categorie;
        this.fournisseurId = fournisseurId;
        this.prixPublic = prixPublic;
        this.disponible = disponible;
    }

    UUID getId() {
        return id;
    }

    String getReference() {
        return reference;
    }

    String getDesignation() {
        return designation;
    }

    String getMarque() {
        return marque;
    }

    CategorieProduit getCategorie() {
        return categorie;
    }

    UUID getFournisseurId() {
        return fournisseurId;
    }

    BigDecimal getPrixPublic() {
        return prixPublic;
    }

    boolean isDisponible() {
        return disponible;
    }
}
