package com.seheli.parametrage.domain;

import java.util.Objects;

/** Ligne de la table {@code parametre} ; la valeur est vérifiée selon son type dès la création. */
public record Parametre(String cle, String valeur, TypeParametre type) {

    public Parametre {
        Objects.requireNonNull(cle, "cle");
        Objects.requireNonNull(type, "type");
        if (valeur == null || valeur.isBlank()) {
            throw new ParametreInvalideException("Paramètre " + cle + " : valeur vide");
        }
        valeur = valeur.strip();
        try {
            type.verifier(valeur);
        } catch (IllegalArgumentException e) {
            throw new ParametreInvalideException(
                    "Paramètre " + cle + " : valeur « " + valeur + " » invalide pour le type " + type, e);
        }
    }
}
