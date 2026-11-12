package com.cic.inscription.application;

/**
 * Formulaire public de pré-inscription tel que tapé (valeurs brutes). Le champ piège anti-robot n'en fait
 * pas partie : il est traité par le contrôleur web.
 */
public record SaisieInscription(String nom, String telephone, String employeur, String typeContrat,
        String trancheSalaire, String produitSouhaite, boolean consentement) {

    public static SaisieInscription vide() {
        return new SaisieInscription("", "", "", "", "", "", false);
    }

    /** Pas de donnée personnelle dans les journaux. */
    @Override
    public String toString() {
        return "SaisieInscription[" + typeContrat + ", " + trancheSalaire + "]";
    }
}
