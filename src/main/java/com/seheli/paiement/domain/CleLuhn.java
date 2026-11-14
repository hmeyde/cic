package com.seheli.paiement.domain;

/**
 * Clé de Luhn d'une suite de chiffres : détecte une faute de frappe sur un chiffre ou l'inversion de
 * deux chiffres voisins dans une référence de paiement.
 */
final class CleLuhn {

    private CleLuhn() {
    }

    static int de(String chiffres) {
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

    /** Vrai si le dernier chiffre est la clé des précédents. */
    static boolean verifie(String chiffresEtCle) {
        String chiffres = chiffresEtCle.substring(0, chiffresEtCle.length() - 1);
        return chiffresEtCle.charAt(chiffresEtCle.length() - 1) - '0' == de(chiffres);
    }
}
