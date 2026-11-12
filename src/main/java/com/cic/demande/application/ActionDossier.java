package com.cic.demande.application;

/** Actions sur un dossier consignées au journal d'audit (code stable, clé de message {@code bo.journal.action.*}). */
public enum ActionDossier {
    SOUMISSION,
    MISE_EN_ANALYSE,
    ACCEPTATION,
    REFUS,
    DEMANDE_JUSTIFICATIF,
    /** Dossier ACCEPTEE annulé par le traitement planifié : apport ou autorisation manquant après le délai. */
    ANNULATION_DELAI_DEPASSE,
    /** Apport encaissé et autorisation de prélèvement ACCEPTEE : le dossier passe à APPORT_ET_AUTORISATION. */
    APPORT_ET_AUTORISATION_RECUS,
    /** Paiement fournisseur validé et bon de commande émis : le dossier passe à COMMANDEE. */
    COMMANDE_FOURNISSEUR
}
