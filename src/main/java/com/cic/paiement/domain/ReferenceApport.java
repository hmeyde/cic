package com.cic.paiement.domain;

import java.util.Objects;

/**
 * Référence unique que le client saisit dans Masrvi en payant son apport : elle permet de rapprocher
 * le paiement reçu de son dossier.
 *
 * <p>Elle est tirée du numéro de contrat, unique par dossier : {@code SEH-2026-000042} donne
 * {@code AP20260000425}, soit {@code AP}, les chiffres du numéro, puis une clé de Luhn qui détecte une
 * faute de frappe sur un chiffre ou l'inversion de deux chiffres voisins. Lettres et chiffres
 * seulement, sans séparateur, pour être saisie sans erreur sur un téléphone.
 */
public record ReferenceApport(String valeur) {

    static final String PREFIXE = "AP";

    public ReferenceApport {
        Objects.requireNonNull(valeur, "valeur");
        if (!estValide(valeur)) {
            throw new IllegalArgumentException("Référence d'apport invalide");
        }
    }

    /** @param numeroContrat numéro du contrat du dossier, par exemple {@code SEH-2026-000042} */
    public static ReferenceApport pourContrat(String numeroContrat) {
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        String chiffres = numeroContrat.replaceAll("\\D", "");
        if (chiffres.isEmpty()) {
            throw new IllegalArgumentException("Numéro de contrat sans chiffres : " + numeroContrat);
        }
        return new ReferenceApport(PREFIXE + chiffres + CleLuhn.de(chiffres));
    }

    /** Chiffres du numéro de contrat dont la référence est tirée, sans le préfixe ni la clé. */
    public String chiffresContrat() {
        return valeur.substring(PREFIXE.length(), valeur.length() - 1);
    }

    /** Vrai si la saisie a la forme d'une référence d'apport et que sa clé est juste. */
    public static boolean estValide(String saisie) {
        if (saisie == null || !saisie.matches(PREFIXE + "\\d{2,}")) {
            return false;
        }
        String chiffres = saisie.substring(PREFIXE.length(), saisie.length() - 1);
        return saisie.charAt(saisie.length() - 1) - '0' == CleLuhn.de(chiffres);
    }

    @Override
    public String toString() {
        return valeur;
    }
}
