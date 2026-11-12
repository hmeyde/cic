package com.cic.paiement.application;

/** Ce qu'est devenue une ligne d'un fichier CSV de paiements à l'import. */
public enum IssueLigne {
    /** Paiement enregistré et rattaché au dossier, du montant de l'apport attendu. */
    CONFORME,
    /** Écart : paiement enregistré et rattaché au dossier, mais d'un montant différent de l'apport. */
    MONTANT_DIFFERENT,
    /** Écart : paiement enregistré, mais sa référence ne désigne aucun dossier. */
    REFERENCE_INCONNUE,
    /** Un paiement porte déjà cette référence : rien n'est enregistré (réimport sans effet). */
    DEJA_ENREGISTRE,
    /** Ligne illisible ou valeur refusée (motif dans {@link LigneImportee#erreur()}) : rien n'est enregistré. */
    INVALIDE;

    public boolean estUnEcart() {
        return this == MONTANT_DIFFERENT || this == REFERENCE_INCONNUE;
    }
}
