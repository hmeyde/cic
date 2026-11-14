package com.seheli.identite.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.seheli.identite.domain.FormatDocument;
import com.seheli.identite.domain.TypeDocument;

/** Métadonnées d'un justificatif ; le client est référencé par identifiant, sans relation JPA. */
@Entity
@Table(name = "document")
class DocumentEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 30, updatable = false)
    private TypeDocument type;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10, updatable = false)
    private FormatDocument format;

    @Column(name = "taille_octets", nullable = false, updatable = false)
    private long tailleOctets;

    @Column(name = "recu_le", nullable = false, updatable = false)
    private Instant recuLe;

    protected DocumentEntity() {
    }

    DocumentEntity(UUID id, UUID clientId, TypeDocument type, FormatDocument format, long tailleOctets,
            Instant recuLe) {
        this.id = id;
        this.clientId = clientId;
        this.type = type;
        this.format = format;
        this.tailleOctets = tailleOctets;
        this.recuLe = recuLe;
    }

    UUID getId() {
        return id;
    }

    UUID getClientId() {
        return clientId;
    }

    TypeDocument getType() {
        return type;
    }

    FormatDocument getFormat() {
        return format;
    }

    long getTailleOctets() {
        return tailleOctets;
    }

    Instant getRecuLe() {
        return recuLe;
    }
}
