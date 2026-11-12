package com.cic.demande.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/** Contrat, fournisseur et produit référencés par identifiant (autres modules, sans relation JPA). */
@Entity
@Immutable
@Table(name = "commande_fournisseur")
class CommandeFournisseurEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "numero", nullable = false, length = 30)
    private String numero;

    @Column(name = "demande_id", nullable = false)
    private UUID demandeId;

    @Column(name = "contrat_id", nullable = false)
    private UUID contratId;

    @Column(name = "numero_contrat", nullable = false, length = 30)
    private String numeroContrat;

    @Column(name = "fournisseur_id", nullable = false)
    private UUID fournisseurId;

    @Column(name = "produit_id", nullable = false)
    private UUID produitId;

    @Column(name = "designation_produit", nullable = false, length = 200)
    private String designationProduit;

    @Column(name = "montant", nullable = false, precision = 12, scale = 0)
    private BigDecimal montant;

    @Column(name = "valide_par", nullable = false, length = 100)
    private String validePar;

    @Column(name = "valide_le", nullable = false)
    private Instant valideLe;

    protected CommandeFournisseurEntity() {
    }

    CommandeFournisseurEntity(UUID id, String numero, UUID demandeId, UUID contratId, String numeroContrat,
            UUID fournisseurId, UUID produitId, String designationProduit, BigDecimal montant, String validePar,
            Instant valideLe) {
        this.id = id;
        this.numero = numero;
        this.demandeId = demandeId;
        this.contratId = contratId;
        this.numeroContrat = numeroContrat;
        this.fournisseurId = fournisseurId;
        this.produitId = produitId;
        this.designationProduit = designationProduit;
        this.montant = montant;
        this.validePar = validePar;
        this.valideLe = valideLe;
    }

    UUID getId() {
        return id;
    }

    String getNumero() {
        return numero;
    }

    UUID getDemandeId() {
        return demandeId;
    }

    UUID getContratId() {
        return contratId;
    }

    String getNumeroContrat() {
        return numeroContrat;
    }

    UUID getFournisseurId() {
        return fournisseurId;
    }

    UUID getProduitId() {
        return produitId;
    }

    String getDesignationProduit() {
        return designationProduit;
    }

    BigDecimal getMontant() {
        return montant;
    }

    String getValidePar() {
        return validePar;
    }

    Instant getValideLe() {
        return valideLe;
    }
}
