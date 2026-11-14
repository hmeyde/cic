package com.seheli.comptabilite.domain;

/**
 * Fait comptabilisé par une écriture. Avec l'identifiant de sa source (paiement, commande, dossier,
 * écriture contrepassée), il identifie l'écriture : un même fait n'est comptabilisé qu'une fois.
 */
public enum NatureEcriture {
    /** Apport reçu sur une référence d'apport ; source : le paiement. */
    APPORT_ENCAISSE,
    /** Prix d'achat payé au fournisseur à la validation de la commande ; source : la commande. */
    PAIEMENT_FOURNISSEUR,
    /** Vente constatée à la remise du produit au client ; source : le dossier. */
    VENTE_LIVREE,
    /** Échéance reçue sur une référence d'échéance ; source : le paiement. */
    ECHEANCE_ENCAISSEE,
    /** Paiement reçu dont la référence ne désigne aucun dossier ; source : le paiement. */
    PAIEMENT_A_IDENTIFIER,
    /** Apport en capital d'un associé ; source : l'apport. */
    APPORT_CAPITAL,
    /** Écriture inverse d'une écriture erronée ; source : l'écriture contrepassée. */
    CONTREPASSATION
}
