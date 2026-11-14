package com.seheli.paiement.application;

import java.util.Objects;
import java.util.Optional;

/**
 * Ligne de données d'un fichier CSV de paiements.
 *
 * @param numero numéro de la ligne dans le fichier, en comptant l'en-tête, à partir de 1
 * @param saisie paiement lu, encore à contrôler comme une saisie manuelle
 * @param erreur code du message si la ligne n'a pas pu être découpée en trois colonnes
 */
public record LigneCsv(int numero, SaisiePaiement saisie, Optional<String> erreur) {

    public LigneCsv {
        Objects.requireNonNull(saisie, "saisie");
        Objects.requireNonNull(erreur, "erreur");
    }
}
