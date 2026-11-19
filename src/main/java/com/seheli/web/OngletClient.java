package com.seheli.web;

import java.util.Optional;

/**
 * Onglets de la barre de navigation de l'espace client. L'onglet actif se déduit du chemin
 * de la page ; les pages de connexion (publiques) n'affichent pas la barre.
 */
public enum OngletClient {
    ACCUEIL, CATALOGUE, DOSSIER, PROFIL;

    /** Onglet actif pour un chemin relatif au contexte, vide hors de l'espace client connecté. */
    public static Optional<OngletClient> pour(String chemin) {
        if (chemin == null) {
            return Optional.empty();
        }
        String c = chemin.endsWith("/") && chemin.length() > 1 ? chemin.substring(0, chemin.length() - 1) : chemin;
        if (c.equals("/client")) {
            return Optional.of(ACCUEIL);
        }
        if (!c.startsWith("/client/") || sousChemin(c, "/client/connexion")) {
            return Optional.empty();
        }
        if (sousChemin(c, "/client/catalogue")) {
            return Optional.of(CATALOGUE);
        }
        if (sousChemin(c, "/client/dossier") || sousChemin(c, "/client/demandes")) {
            return Optional.of(DOSSIER);
        }
        if (sousChemin(c, "/client/profil") || sousChemin(c, "/client/emploi")
                || sousChemin(c, "/client/justificatifs")) {
            return Optional.of(PROFIL);
        }
        return Optional.empty();
    }

    private static boolean sousChemin(String chemin, String prefixe) {
        return chemin.equals(prefixe) || chemin.startsWith(prefixe + "/");
    }
}
