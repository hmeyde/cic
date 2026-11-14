package com.seheli.demande.domain;

/**
 * Grandes étapes du parcours telles que le client les voit, dans l'ordre. Plusieurs statuts
 * peuvent correspondre à la même étape.
 */
public enum EtapeDossier {
    /** Étude de la demande par l'analyste : DEMANDE_SOUMISE, EN_ANALYSE (REFUSEE s'y arrête). */
    ETUDE,
    /** Offre acceptée : apport et autorisation de prélèvement attendus (ANNULEE s'y arrête). */
    APPORT_ET_AUTORISATION,
    /** Apport et autorisation reçus : Seheli commande le produit au fournisseur. */
    COMMANDE,
    /** Produit commandé : le client le reçoit et confirme la remise. */
    REMISE,
    /** Mensualités prélevées le jour de paie : LIVREE, EN_REMBOURSEMENT, EN_RETARD, RECOUVREMENT. */
    REMBOURSEMENT;

    /** Étape en cours pour ce statut ; pour SOLDEE, la dernière (alors terminée). */
    public static EtapeDossier de(StatutDossier statut) {
        return switch (statut) {
            case DEMANDE_SOUMISE, EN_ANALYSE, REFUSEE -> ETUDE;
            case ACCEPTEE, ANNULEE -> APPORT_ET_AUTORISATION;
            case APPORT_ET_AUTORISATION -> COMMANDE;
            case COMMANDEE -> REMISE;
            case LIVREE, EN_REMBOURSEMENT, EN_RETARD, RECOUVREMENT, SOLDEE -> REMBOURSEMENT;
        };
    }
}
