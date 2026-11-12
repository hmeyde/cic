package com.cic.paiement.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.cic.paiement.domain.StatutRapprochement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Paiement reçu, immuable. Contrat, dossier et client référencés par identifiant (autres modules,
 * sans relation JPA).
 */
@Entity
@Table(name = "paiement")
class PaiementEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "reference", nullable = false, updatable = false, length = 30)
    private String reference;

    @Column(name = "montant", nullable = false, updatable = false, precision = 14, scale = 0)
    private BigDecimal montant;

    @Column(name = "recu_le", nullable = false, updatable = false)
    private LocalDate recuLe;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, updatable = false, length = 20)
    private StatutRapprochement statut;

    @Column(name = "contrat_id", updatable = false)
    private UUID contratId;

    @Column(name = "numero_contrat", updatable = false, length = 30)
    private String numeroContrat;

    @Column(name = "demande_id", updatable = false)
    private UUID demandeId;

    @Column(name = "client_id", updatable = false)
    private UUID clientId;

    @Column(name = "montant_attendu", updatable = false, precision = 14, scale = 0)
    private BigDecimal montantAttendu;

    @Column(name = "saisi_par", nullable = false, updatable = false, length = 100)
    private String saisiPar;

    @Column(name = "saisi_le", nullable = false, updatable = false)
    private Instant saisiLe;

    protected PaiementEntity() {
    }

    PaiementEntity(UUID id, String reference, BigDecimal montant, LocalDate recuLe, StatutRapprochement statut,
            UUID contratId, String numeroContrat, UUID demandeId, UUID clientId, BigDecimal montantAttendu,
            String saisiPar, Instant saisiLe) {
        this.id = id;
        this.reference = reference;
        this.montant = montant;
        this.recuLe = recuLe;
        this.statut = statut;
        this.contratId = contratId;
        this.numeroContrat = numeroContrat;
        this.demandeId = demandeId;
        this.clientId = clientId;
        this.montantAttendu = montantAttendu;
        this.saisiPar = saisiPar;
        this.saisiLe = saisiLe;
    }

    UUID getId() {
        return id;
    }

    String getReference() {
        return reference;
    }

    BigDecimal getMontant() {
        return montant;
    }

    LocalDate getRecuLe() {
        return recuLe;
    }

    StatutRapprochement getStatut() {
        return statut;
    }

    UUID getContratId() {
        return contratId;
    }

    String getNumeroContrat() {
        return numeroContrat;
    }

    UUID getDemandeId() {
        return demandeId;
    }

    UUID getClientId() {
        return clientId;
    }

    BigDecimal getMontantAttendu() {
        return montantAttendu;
    }

    String getSaisiPar() {
        return saisiPar;
    }

    Instant getSaisiLe() {
        return saisiLe;
    }
}
