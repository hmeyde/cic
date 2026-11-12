package com.cic.comptabilite.adapter.out.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import com.cic.comptabilite.domain.NatureEcriture;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Écriture comptable, en lecture seule : les écritures sont insérées par requête native (une seule
 * fois par fait) et jamais modifiées. Dossier et contrat référencés par identifiant (autres modules).
 */
@Entity
@Immutable
@Table(name = "ecriture_comptable")
class EcritureComptableEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false, length = 30)
    private NatureEcriture nature;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "date_ecriture", nullable = false)
    private LocalDate dateEcriture;

    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;

    @Column(name = "demande_id")
    private UUID demandeId;

    @Column(name = "contrat_id")
    private UUID contratId;

    @Column(name = "passee_le", nullable = false)
    private Instant passeeLe;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ligne_ecriture", joinColumns = @JoinColumn(name = "ecriture_id"))
    @OrderColumn(name = "position")
    private List<LigneEcritureEmbeddable> lignes = new ArrayList<>();

    protected EcritureComptableEntity() {
    }

    UUID getId() {
        return id;
    }

    NatureEcriture getNature() {
        return nature;
    }

    UUID getSourceId() {
        return sourceId;
    }

    LocalDate getDateEcriture() {
        return dateEcriture;
    }

    String getLibelle() {
        return libelle;
    }

    UUID getDemandeId() {
        return demandeId;
    }

    UUID getContratId() {
        return contratId;
    }

    Instant getPasseeLe() {
        return passeeLe;
    }

    List<LigneEcritureEmbeddable> getLignes() {
        return lignes;
    }
}
