package com.cic.identite.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Justificatif envoyé par un client : pièce d'identité, contrat ou attestation de travail, bulletin
 * de paie, RIB.
 *
 * <p>Seules les métadonnées sont ici ; le contenu est rangé par le port {@link StockageDocuments}
 * sous l'identifiant du document, hors du dossier public de l'application. Le nom d'origine du
 * fichier n'est pas conservé (il peut contenir des données personnelles). PDF, JPEG ou PNG,
 * 5 Mo au plus.
 */
public final class Document {

    /** Taille maximale d'un justificatif : 5 Mo. */
    public static final int TAILLE_MAX_MO = 5;
    public static final long TAILLE_MAX_OCTETS = TAILLE_MAX_MO * 1024L * 1024L;

    private final UUID id;
    private final UUID clientId;
    private final TypeDocument type;
    private final FormatDocument format;
    private final long tailleOctets;
    private final Instant recuLe;

    private Document(UUID id, UUID clientId, TypeDocument type, FormatDocument format, long tailleOctets,
            Instant recuLe) {
        this.id = Objects.requireNonNull(id, "id");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.type = Objects.requireNonNull(type, "type");
        this.format = Objects.requireNonNull(format, "format");
        this.recuLe = Objects.requireNonNull(recuLe, "recuLe");
        if (tailleOctets < 1 || tailleOctets > TAILLE_MAX_OCTETS) {
            throw new IllegalArgumentException("Taille de document hors bornes : " + tailleOctets);
        }
        this.tailleOctets = tailleOctets;
    }

    public static Document nouveau(UUID clientId, TypeDocument type, FormatDocument format, long tailleOctets,
            Instant recuLe) {
        return new Document(UUID.randomUUID(), clientId, type, format, tailleOctets, recuLe);
    }

    /** Reconstitution depuis le stockage. */
    public static Document reconstituer(UUID id, UUID clientId, TypeDocument type, FormatDocument format,
            long tailleOctets, Instant recuLe) {
        return new Document(id, clientId, type, format, tailleOctets, recuLe);
    }

    /** Ce document appartient à ce client. */
    public boolean appartientA(UUID clientId) {
        return this.clientId.equals(clientId);
    }

    public UUID id() {
        return id;
    }

    public UUID clientId() {
        return clientId;
    }

    public TypeDocument type() {
        return type;
    }

    public FormatDocument format() {
        return format;
    }

    public long tailleOctets() {
        return tailleOctets;
    }

    public Instant recuLe() {
        return recuLe;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Document d && id.equals(d.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Document[" + id + ", client " + clientId + ", " + type + ", " + format + "]";
    }
}
