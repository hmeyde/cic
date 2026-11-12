package com.cic.comptabilite.domain;

/**
 * Plan de comptes simplifié du MVP (écritures de base seulement), numéroté comme le plan comptable
 * général.
 *
 * <p>Schéma d'un dossier de l'exemple chiffré : apport encaissé 8 320 (512 / 4191) ; fournisseur payé
 * 18 400 (607 / 512) ; vente constatée à la remise 20 800 (4191 8 320 + 411 12 480 / 707) ; chaque
 * échéance encaissée 2 080 (512 / 411). Au solde, 411 et 4191 sont à zéro et la banque garde le
 * bénéfice brut de 2 400 (707 − 607).
 */
public enum Compte {
    /** Compte Masrvi de CIC : tout l'argent reçu ou versé y passe. */
    BANQUE("512", "Banque — compte Masrvi"),
    /** Reste dû par les clients sur les ventes à tempérament. */
    CLIENTS("411", "Clients — ventes à tempérament"),
    /** Apports reçus avant la remise du produit (avances des clients). */
    CLIENTS_APPORTS("4191", "Clients — apports reçus"),
    /** Paiements reçus dont la référence ne désigne aucun dossier, en attente d'identification. */
    PAIEMENTS_A_IDENTIFIER("471", "Paiements à identifier"),
    /** Prix d'achat payé au fournisseur. */
    ACHATS("607", "Achats de marchandises"),
    /** Prix de vente au client (prix public + marge CIC), fixé à la signature. */
    VENTES("707", "Ventes de marchandises");

    private final String numero;
    private final String intitule;

    Compte(String numero, String intitule) {
        this.numero = numero;
        this.intitule = intitule;
    }

    public String numero() {
        return numero;
    }

    public String intitule() {
        return intitule;
    }

    /** @throws IllegalArgumentException aucun compte ne porte ce numéro */
    public static Compte deNumero(String numero) {
        for (Compte c : values()) {
            if (c.numero.equals(numero)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Compte inconnu : " + numero);
    }
}
