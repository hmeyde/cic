package com.cic.paiement.application;

import java.util.Objects;
import java.util.Optional;

import com.cic.paiement.domain.Paiement;

/**
 * Résultat de l'import d'une ligne du fichier CSV.
 *
 * @param numero   numéro de la ligne dans le fichier
 * @param lue      valeurs telles que lues dans le fichier
 * @param paiement paiement enregistré par cette ligne, ou déjà enregistré sous sa référence ; vide si la
 *                 ligne est invalide
 * @param erreur   code du message expliquant le refus d'une ligne invalide ; vide pour les autres issues
 */
public record LigneImportee(int numero, SaisiePaiement lue, IssueLigne issue, Optional<Paiement> paiement,
        Optional<String> erreur) {

    public LigneImportee {
        Objects.requireNonNull(lue, "lue");
        Objects.requireNonNull(issue, "issue");
        Objects.requireNonNull(paiement, "paiement");
        Objects.requireNonNull(erreur, "erreur");
    }
}
