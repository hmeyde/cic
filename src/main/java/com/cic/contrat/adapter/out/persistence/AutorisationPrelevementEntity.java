package com.cic.contrat.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import com.cic.contrat.domain.StatutAutorisation;
import com.cic.contrat.domain.TypeAutorisation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Le contrat est référencé par identifiant (même module, clé étrangère en base) ; le dossier et le
 * client par identifiant (autres modules).
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
    @Column(name = "type", nullable = false, length = 20)
    private TypeAutorisation type;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAutorisation statut;

    @Column(name = "emise_le", nullable = false, updatable = false)
    private Instant emiseLe;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AutorisationPrelevementEntity() {
    }

    AutorisationPrelevementEntity(UUID id, UUID contratId, UUID demandeId, UUID clientId, TypeAutorisation type,
            StatutAutorisation statut, Instant emiseLe) {
        this.id = id;
        this.contratId = contratId;
        this.demandeId = demandeId;
        this.clientId = clientId;
        this.type = type;
        this.statut = statut;
        this.emiseLe = emiseLe;
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
}
