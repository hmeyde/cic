package com.cic.paiement.domain;

import java.util.Objects;

/**
 * Référence unique d'une échéance, portée par le prélèvement (virement permanent, domiciliation ou
 * retenue sur salaire) qui la règle : elle permet de rapprocher le paiement reçu de l'échéance.
 *
 * <p>Tirée du numéro de contrat et du rang : l'échéance 3 du contrat {@code SEH-2026-000042} donne
 * {@code EC2026000042037}, soit {@code EC}, les chiffres du numéro, le rang sur deux chiffres, puis une
 * clé de Luhn sur l'ensemble. Lettres et chiffres seulement, comme la {@link ReferenceApport}.
 */
public record ReferenceEcheance(String valeur) {

    static final String PREFIXE = "EC";
    static final int CHIFFRES_RANG = 2;
    static final int RANG_MAX = 99;

    public ReferenceEcheance {
        Objects.requireNonNull(valeur, "valeur");
        if (!estValide(valeur)) {
            throw new IllegalArgumentException("Référence d'échéance invalide");
        }
    }

    /** @param numeroContrat numéro du contrat, par exemple {@code SEH-2026-000042} */
    public static ReferenceEcheance pourEcheance(String numeroContrat, int rang) {
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        String chiffres = numeroContrat.replaceAll("\\D", "");
        if (chiffres.isEmpty()) {
            throw new IllegalArgumentException("Numéro de contrat sans chiffres : " + numeroContrat);
        }
        if (rang <= 0 || rang > RANG_MAX) {
            throw new IllegalArgumentException("Rang d'échéance invalide : " + rang);
        }
        String corps = chiffres + "%02d".formatted(rang);
        return new ReferenceEcheance(PREFIXE + corps + CleLuhn.de(corps));
    }

    /** Chiffres du numéro de contrat dont la référence est tirée. */
    public String chiffresContrat() {
        return valeur.substring(PREFIXE.length(), valeur.length() - 1 - CHIFFRES_RANG);
    }

    /** Rang de l'échéance désignée. */
    public int rang() {
        return Integer.parseInt(valeur.substring(valeur.length() - 1 - CHIFFRES_RANG, valeur.length() - 1));
    }

    /** Vrai si la saisie a la forme d'une référence d'échéance, avec un rang positif et une clé juste. */
    public static boolean estValide(String saisie) {
        if (saisie == null || !saisie.matches(PREFIXE + "\\d{" + (CHIFFRES_RANG + 2) + ",}")) {
            return false;
        }
        String corps = saisie.substring(PREFIXE.length());
        String rang = corps.substring(corps.length() - 1 - CHIFFRES_RANG, corps.length() - 1);
        return !rang.equals("00") && CleLuhn.verifie(corps);
    }

    @Override
    public String toString() {
        return valeur;
    }
}
