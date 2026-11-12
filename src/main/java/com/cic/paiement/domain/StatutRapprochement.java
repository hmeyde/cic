package com.cic.paiement.domain;

/** Résultat du rapprochement d'un paiement reçu avec un dossier. */
public enum StatutRapprochement {
    /** La référence désigne un dossier : le paiement lui est rattaché. */
    RAPPROCHE,
    /** La référence ne désigne aucun dossier (faute de frappe, référence inconnue) : paiement à rattacher. */
    NON_RAPPROCHE
}
