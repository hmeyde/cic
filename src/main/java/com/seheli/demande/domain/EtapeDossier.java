package com.seheli.demande.domain;

/**
 * Jalons du parcours tels que le client les voit, dans l'ordre, comme un suivi de livraison. Chaque
 * jalon correspond au statut atteint ; plusieurs statuts peuvent tomber sur le même jalon.
 */
public enum EtapeDossier {
    /** Demande envoyée : DEMANDE_SOUMISE. */
    SOUMISE,
    /** Étude par l'analyste : EN_ANALYSE (REFUSEE s'arrête juste après). */
    EN_ANALYSE,
    /** Offre acceptée : apport et autorisation de prélèvement attendus sous 7 jours (ANNULEE s'arrête juste après). */
    ACCEPTEE,
    /** Apport encaissé et autorisation acceptée par la banque : Seheli commande le produit. */
    APPORT_ET_AUTORISATION,
    /** Produit commandé au fournisseur : le client le reçoit et confirme la remise. */
    COMMANDEE,
    /** Produit remis au client. */
    LIVREE,
    /** Mensualités prélevées le jour de paie : EN_REMBOURSEMENT, EN_RETARD, RECOUVREMENT. */
    EN_REMBOURSEMENT,
    /** Toutes les échéances payées. */
    SOLDEE;

    /**
     * Jalon courant pour ce statut : le dernier atteint, ou, pour un dossier refusé ou annulé, le
     * jalon qui n'a pas été atteint (l'étape où le dossier s'est arrêté).
     */
    public static EtapeDossier de(StatutDossier statut) {
        return switch (statut) {
            case DEMANDE_SOUMISE -> SOUMISE;
            case EN_ANALYSE -> EN_ANALYSE;
            case ACCEPTEE, REFUSEE -> ACCEPTEE;
            case APPORT_ET_AUTORISATION, ANNULEE -> APPORT_ET_AUTORISATION;
            case COMMANDEE -> COMMANDEE;
            case LIVREE -> LIVREE;
            case EN_REMBOURSEMENT, EN_RETARD, RECOUVREMENT -> EN_REMBOURSEMENT;
            case SOLDEE -> SOLDEE;
        };
    }
}
