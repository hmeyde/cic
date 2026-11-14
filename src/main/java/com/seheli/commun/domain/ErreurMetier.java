package com.seheli.commun.domain;

import java.util.Objects;

/**
 * Violation d'une règle de gestion. Se distingue d'une erreur de programmation
 * ({@link IllegalArgumentException}) : elle est attendue, expliquée au client ou à l'analyste
 * et porte un code stable, utilisable comme clé de message i18n.
 */
public abstract class ErreurMetier extends RuntimeException {

    private final String code;

    protected ErreurMetier(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    /** Code stable, par exemple {@code echeancier.endettement.depasse}. */
    public String code() {
        return code;
    }
}
