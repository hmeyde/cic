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
    COMMANDE_FOURNISSEUR,
    /** Le client confirme avoir reçu son produit : le dossier passe à LIVREE. */
    CONFIRMATION_REMISE,
    /** Produit remis : le dossier passe à EN_REMBOURSEMENT, les échéances du contrat sont prélevées. */
    DEBUT_REMBOURSEMENT,
    /** Échéance impayée à sa date, constatée par le traitement planifié : le dossier passe à EN_RETARD. */
    PASSAGE_EN_RETARD
}
