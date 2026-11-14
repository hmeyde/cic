package com.seheli.audit.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Objet sur lequel porte une action : son type (par exemple {@code DEMANDE}) et son identifiant
 * technique dans le module propriétaire.
 */
public record ObjetAudite(String type, UUID id) {

    static final int LONGUEUR_TYPE_MAX = 50;

    public ObjetAudite {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
        if (type.isBlank() || type.length() > LONGUEUR_TYPE_MAX) {
            throw new IllegalArgumentException("Type d'objet audité vide ou trop long");
        }
    }
}
