package com.seheli.contrat.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.seheli.identite.domain.FormatDocument;

/**
 * Autorisation signée par le client, numérisée et déposée au back-office. Seules les métadonnées
 * sont ici ; le contenu est rangé par le port {@link StockageDocumentsSignes} sous {@link #id()}.
 * PDF, JPEG ou PNG (reconnu au contenu), 5 Mo au plus, comme les justificatifs.
 *
 * @param deposePar identifiant de connexion du membre du back-office
 */
public record DocumentSigne(UUID id, FormatDocument format, long tailleOctets, Instant deposeLe, String deposePar) {

    public static final long TAILLE_MAX_OCTETS = com.seheli.identite.domain.Document.TAILLE_MAX_OCTETS;

    public DocumentSigne {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(deposeLe, "deposeLe");
        Objects.requireNonNull(deposePar, "deposePar");
        if (tailleOctets < 1 || tailleOctets > TAILLE_MAX_OCTETS) {
            throw new IllegalArgumentException("Taille de document hors bornes : " + tailleOctets);
        }
        if (deposePar.isBlank()) {
            throw new IllegalArgumentException("Auteur du dépôt vide");
        }
    }

    public static DocumentSigne nouveau(FormatDocument format, long tailleOctets, Instant deposeLe, String deposePar) {
        return new DocumentSigne(UUID.randomUUID(), format, tailleOctets, deposeLe, deposePar);
    }
}
