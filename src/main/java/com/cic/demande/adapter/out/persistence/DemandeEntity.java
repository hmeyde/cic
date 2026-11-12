package com.cic.demande.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.cic.demande.domain.StatutDossier;

/** Client, produit et fournisseur référencés par identifiant (autres modules, sans relation JPA). */
@Entity
@Table(name = "demande")
class DemandeEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "produit_id", nullable = false, updatable = false)
    private UUID produitId;

    @Column(name = "fournisseur_id", nullable = false, updatable = false)
    private UUID fournisseurId;

    @Column(name = "designation_produit", nullable = false, updatable = false, length = 200)
    private String designationProduit;

    @Column(name = "prix_public", nullable = false, updatable = false, precision = 12, scale = 0)
    private BigDecimal prixPublic;

    @Column(name = "prix_achat", nullable = false, updatable = false, precision = 12, scale = 0)
    private BigDecimal prixAchat;

    @Column(name = "prix_vente", nullable = false, updatable = false, precision = 12, scale = 0)
    private BigDecimal prixVente;

    @Column(name = "apport_minimum", nullable = false, updatable = false, precision = 12, scale = 0)
    private BigDecimal apportMinimum;

    @Column(name = "apport", nullable = false, updatable = false, precision = 12, scale = 0)
    private BigDecimal apport;

    @Column(name = "nombre_mois", nullable = false, updatable = false)
    private int nombreMois;

    @Column(name = "mensualite", nullable = false, updatable = false, precision = 12, scale = 0)
    private BigDecimal mensualite;

    @Column(name = "derniere_mensualite", nullable = false, updatable = false, precision = 12, scale = 0)
    private BigDecimal derniereMensualite;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutDossier statut;

    @Column(name = "soumise_le", nullable = false, updatable = false)
    private Instant soumiseLe;

    @Column(name = "statut_change_le", nullable = false)
    private Instant statutChangeLe;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected DemandeEntity() {
    }

    DemandeEntity(UUID id, UUID clientId, UUID produitId, UUID fournisseurId, String designationProduit,
            BigDecimal prixPublic, BigDecimal prixAchat, BigDecimal prixVente, BigDecimal apportMinimum,
            BigDecimal apport, int nombreMois, BigDecimal mensualite, BigDecimal derniereMensualite,
            StatutDossier statut, Instant soumiseLe, Instant statutChangeLe) {
        this.id = id;
        this.clientId = clientId;
        this.produitId = produitId;
        this.fournisseurId = fournisseurId;
        this.designationProduit = designationProduit;
        this.prixPublic = prixPublic;
        this.prixAchat = prixAchat;
        this.prixVente = prixVente;
        this.apportMinimum = apportMinimum;
        this.apport = apport;
        this.nombreMois = nombreMois;
        this.mensualite = mensualite;
        this.derniereMensualite = derniereMensualite;
        this.statut = statut;
        this.soumiseLe = soumiseLe;
        this.statutChangeLe = statutChangeLe;
    }

    /** Seuls le statut et sa date changent après la soumission. */
    void changerStatut(StatutDossier nouveau, Instant le) {
        this.statut = nouveau;
        this.statutChangeLe = le;
    }

    UUID getId() {
        return id;
    }

    UUID getClientId() {
        return clientId;
    }

    UUID getProduitId() {
        return produitId;
    }

    UUID getFournisseurId() {
        return fournisseurId;
    }

    String getDesignationProduit() {
        return designationProduit;
    }

    BigDecimal getPrixPublic() {
        return prixPublic;
    }

    BigDecimal getPrixAchat() {
        return prixAchat;
    }

    BigDecimal getPrixVente() {
        return prixVente;
    }

    BigDecimal getApportMinimum() {
        return apportMinimum;
    }

    BigDecimal getApport() {
        return apport;
    }

    int getNombreMois() {
        return nombreMois;
    }

    BigDecimal getMensualite() {
        return mensualite;
    }

    BigDecimal getDerniereMensualite() {
        return derniereMensualite;
    }

    StatutDossier getStatut() {
        return statut;
    }

    Instant getSoumiseLe() {
        return soumiseLe;
    }

    Instant getStatutChangeLe() {
        return statutChangeLe;
    }
}
