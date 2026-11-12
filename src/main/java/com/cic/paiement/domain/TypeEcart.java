package com.cic.paiement.domain;

/** Écart constaté entre un paiement reçu et ce qu'attendait CIC, à traiter par le back-office. */
public enum TypeEcart {
    /** La référence désigne un dossier, mais le montant reçu n'est pas l'apport fixé au contrat. */
    MONTANT_DIFFERENT,
    /** La référence ne désigne aucun dossier : le paiement n'est rattaché à rien. */
    REFERENCE_INCONNUE
}
