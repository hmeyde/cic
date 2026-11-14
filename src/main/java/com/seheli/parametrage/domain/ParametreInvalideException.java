package com.seheli.parametrage.domain;

/** Paramètre manquant, d'un type inattendu ou dont la valeur ne respecte pas son type. */
public class ParametreInvalideException extends RuntimeException {

    public ParametreInvalideException(String message) {
        super(message);
    }

    public ParametreInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}
