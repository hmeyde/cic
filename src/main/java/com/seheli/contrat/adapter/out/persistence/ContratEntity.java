package com.seheli.contrat.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Dossier, client, produit et fournisseur référencés par identifiant (autres modules, sans relation
 * JPA). Les échéances appartiennent au contrat (même module). Prix et apport ne changent jamais.
 */
@Entity
@Table(name = "contrat")
class ContratEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "numero", nullable = false, updatable = false, length = 30)
    private String numero;

    @Column(name = "demande_id", nullable = false, updatable = false)
    private UUID demandeId;

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

    @Column(name = "etabli_le", nullable = false, updatable = false)
    private Instant etabliLe;

    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rang")
    private List<EcheanceEntity> echeances = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ContratEntity() {
    }

    ContratEntity(UUID id, String numero, UUID demandeId, UUID clientId, UUID produitId, UUID fournisseurId,
            String designationProduit, BigDecimal prixPublic, BigDecimal prixAchat, BigDecimal prixVente,
            BigDecimal apportMinimum, BigDecimal apport, int nombreMois, Instant etabliLe) {
        this.id = id;
        this.numero = numero;
        this.demandeId = demandeId;
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
        this.etabliLe = etabliLe;
    }

    void ajouter(EcheanceEntity echeance) {
        echeances.add(echeance);
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

    Instant getEtabliLe() {
        return etabliLe;
    }

    List<EcheanceEntity> getEcheances() {
        return echeances;
    }
}
