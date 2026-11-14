package com.seheli.contrat.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.seheli.contrat.domain.StatutEcheance;

/** Échéance d'un contrat : seul le statut change après la signature. */
@Entity
@Table(name = "echeance")
class EcheanceEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrat_id", nullable = false, updatable = false)
    private ContratEntity contrat;

    @Column(name = "rang", nullable = false, updatable = false)
    private int rang;

    @Column(name = "date_echeance", nullable = false, updatable = false)
    private LocalDate dateEcheance;

    @Column(name = "montant", nullable = false, updatable = false, precision = 12, scale = 0)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutEcheance statut;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EcheanceEntity() {
    }

    EcheanceEntity(UUID id, ContratEntity contrat, int rang, LocalDate dateEcheance, BigDecimal montant,
            StatutEcheance statut) {
        this.id = id;
        this.contrat = contrat;
        this.rang = rang;
        this.dateEcheance = dateEcheance;
        this.montant = montant;
        this.statut = statut;
    }

    UUID getId() {
        return id;
    }

    int getRang() {
        return rang;
    }

    LocalDate getDateEcheance() {
        return dateEcheance;
    }

    BigDecimal getMontant() {
        return montant;
    }

    StatutEcheance getStatut() {
        return statut;
    }

    UUID getContratId() {
        return contrat.getId();
    }

    void changerStatut(StatutEcheance nouveau) {
        this.statut = nouveau;
    }
}
