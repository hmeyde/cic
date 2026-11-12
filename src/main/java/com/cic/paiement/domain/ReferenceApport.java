package com.cic.paiement.domain;

import java.util.Objects;

/**
 * Référence unique que le client saisit dans Masrvi en payant son apport : elle permet de rapprocher
 * le paiement reçu de son dossier.
 *
 * <p>Elle est tirée du numéro de contrat, unique par dossier : {@code CIC-2026-000042} donne
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

    /** @param numeroContrat numéro du contrat du dossier, par exemple {@code CIC-2026-000042} */
    public static ReferenceApport pourContrat(String numeroContrat) {
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        String chiffres = numeroContrat.replaceAll("\\D", "");
        if (chiffres.isEmpty()) {
            throw new IllegalArgumentException("Numéro de contrat sans chiffres : " + numeroContrat);
        }
        return new ReferenceApport(PREFIXE + chiffres + cleLuhn(chiffres));
    }

    /** Vrai si la saisie a la forme d'une référence d'apport et que sa clé est juste. */
    public static boolean estValide(String saisie) {
        if (saisie == null || !saisie.matches(PREFIXE + "\\d{2,}")) {
            return false;
        }
        String chiffres = saisie.substring(PREFIXE.length(), saisie.length() - 1);
        return saisie.charAt(saisie.length() - 1) - '0' == cleLuhn(chiffres);
    }

    private static int cleLuhn(String chiffres) {
        int somme = 0;
        boolean doubler = true;
        for (int i = chiffres.length() - 1; i >= 0; i--) {
            int c = chiffres.charAt(i) - '0';
            if (doubler) {
                c *= 2;
                if (c > 9) {
                    c -= 9;
                }
            }
            somme += c;
            doubler = !doubler;
        }
        return (10 - somme % 10) % 10;
    }

    @Override
    public String toString() {
        return valeur;
    }
}
