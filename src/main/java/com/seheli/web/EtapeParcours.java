package com.seheli.web;

import java.util.Optional;

/**
 * Étapes du parcours de demande, dans l'ordre : Compte, Emploi, Justificatifs, Demande. L'étape
 * affichée se déduit du chemin de la page ; les autres pages n'affichent pas la barre de progression.
 */
public enum EtapeParcours {
    COMPTE(null), EMPLOI("/client/emploi"), JUSTIFICATIFS("/client/justificatifs"), DEMANDE("/client/catalogue");

    private final String chemin;

    EtapeParcours(String chemin) {
        this.chemin = chemin;
    }

    /** Page de l'étape, {@code null} pour le compte (créé une seule fois, il ne se rouvre pas). */
    public String chemin() {
        return chemin;
    }

    /** Numéro affiché, de 1 à 4. */
    public int numero() {
        return ordinal() + 1;
    }

    /** Étape du parcours pour un chemin relatif au contexte, vide hors du parcours. */
    public static Optional<EtapeParcours> pour(String chemin) {
        if (chemin == null) {
            return Optional.empty();
        }
        String c = chemin.endsWith("/") && chemin.length() > 1 ? chemin.substring(0, chemin.length() - 1) : chemin;
        if (c.equals("/client/connexion/compte")) {
            return Optional.of(COMPTE);
        }
        if (c.equals("/client/emploi")) {
            return Optional.of(EMPLOI);
        }
        if (c.equals("/client/justificatifs")) {
            return Optional.of(JUSTIFICATIFS);
        }
        // Catalogue, fiche produit et confirmation de la demande ; pas le fragment HTMX de simulation.
        if (c.equals("/client/catalogue")
                || c.matches("/client/catalogue/[^/]+") || c.matches("/client/catalogue/[^/]+/demande")) {
            return Optional.of(DEMANDE);
        }
        return Optional.empty();
    }
}
