package com.seheli.audit.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Auteur d'une action journalisée. L'identifiant est technique : identifiant technique du client,
 * identifiant de connexion du membre du back-office ou nom du traitement, jamais un nom ni un NNI.
 */
public record Acteur(TypeActeur type, String identifiant) {

    static final int LONGUEUR_MAX = 100;

    public Acteur {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(identifiant, "identifiant");
        if (identifiant.isBlank() || identifiant.length() > LONGUEUR_MAX) {
            throw new IllegalArgumentException("Identifiant d'acteur vide ou trop long");
        }
    }

    public static Acteur client(UUID clientId) {
        return new Acteur(TypeActeur.CLIENT, clientId.toString());
    }

    public static Acteur backOffice(String identifiant) {
        return new Acteur(TypeActeur.BACK_OFFICE, identifiant);
    }

    public static Acteur systeme(String traitement) {
        return new Acteur(TypeActeur.SYSTEME, traitement);
    }
}
