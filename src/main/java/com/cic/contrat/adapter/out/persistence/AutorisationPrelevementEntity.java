package com.cic.contrat.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import com.cic.contrat.domain.StatutAutorisation;
import com.cic.contrat.domain.TypeAutorisation;
import com.cic.identite.domain.FormatDocument;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Le contrat est référencé par identifiant (même module, clé étrangère en base) ; le dossier et le
 * client par identifiant (autres modules). Les colonnes {@code document_signe_*} sont toutes vides
 * tant que l'autorisation est A_SIGNER.
 */
@Entity
@Table(name = "autorisation_prelevement")
class AutorisationPrelevementEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "contrat_id", nullable = false, updatable = false)
    private UUID contratId;

    @Column(name = "demande_id", nullable = false, updatable = false)
    private UUID demandeId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private TypeAutorisation type;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAutorisation statut;

    @Column(name = "emise_le", nullable = false, updatable = false)
    private Instant emiseLe;

    @Column(name = "statut_change_le", nullable = false)
    private Instant statutChangeLe;

    @Column(name = "document_signe_id")
    private UUID documentSigneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_signe_format", length = 10)
    private FormatDocument documentSigneFormat;

    @Column(name = "document_signe_taille")
    private Long documentSigneTaille;

    @Column(name = "document_signe_depose_le")
    private Instant documentSigneDeposeLe;

    @Column(name = "document_signe_depose_par", length = 100)
    private String documentSigneDeposePar;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AutorisationPrelevementEntity() {
    }

    AutorisationPrelevementEntity(UUID id, UUID contratId, UUID demandeId, UUID clientId, TypeAutorisation type,
            Instant emiseLe) {
        this.id = id;
        this.contratId = contratId;
        this.demandeId = demandeId;
        this.clientId = clientId;
        this.type = type;
        this.emiseLe = emiseLe;
    }

    void suivi(StatutAutorisation statut, Instant statutChangeLe, UUID documentSigneId,
            FormatDocument documentSigneFormat, Long documentSigneTaille, Instant documentSigneDeposeLe,
            String documentSigneDeposePar) {
        this.statut = statut;
        this.statutChangeLe = statutChangeLe;
        this.documentSigneId = documentSigneId;
        this.documentSigneFormat = documentSigneFormat;
        this.documentSigneTaille = documentSigneTaille;
        this.documentSigneDeposeLe = documentSigneDeposeLe;
        this.documentSigneDeposePar = documentSigneDeposePar;
    }

    UUID getId() {
        return id;
    }

    UUID getContratId() {
        return contratId;
    }

    UUID getDemandeId() {
        return demandeId;
    }

    UUID getClientId() {
        return clientId;
    }

    TypeAutorisation getType() {
        return type;
    }

    StatutAutorisation getStatut() {
        return statut;
    }

    Instant getEmiseLe() {
        return emiseLe;
    }

    Instant getStatutChangeLe() {
        return statutChangeLe;
    }

    UUID getDocumentSigneId() {
        return documentSigneId;
    }

    FormatDocument getDocumentSigneFormat() {
        return documentSigneFormat;
    }

    Long getDocumentSigneTaille() {
        return documentSigneTaille;
    }

    Instant getDocumentSigneDeposeLe() {
        return documentSigneDeposeLe;
    }

    String getDocumentSigneDeposePar() {
        return documentSigneDeposePar;
    }
}
