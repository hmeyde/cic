package com.seheli.demande.domain;

/**
 * Ce qui doit se passer ensuite sur un dossier, et qui doit agir : le client ou Seheli.
 */
public enum ProchaineEtape {
    ATTENDRE_DECISION(false),
    PAYER_APPORT_ET_SIGNER_AUTORISATION(true),
    ATTENDRE_COMMANDE(false),
    RECEVOIR_ET_CONFIRMER_REMISE(true),
    ATTENDRE_PREMIER_PRELEVEMENT(false),
    PROVISIONNER_COMPTE(true),
    REGULARISER_ECHEANCE(true),
    CONTACTER_CIC(true),
    NOUVELLE_DEMANDE_POSSIBLE(true);

    private final boolean parClient;

    ProchaineEtape(boolean parClient) {
        this.parClient = parClient;
    }

    /** Vrai si c'est au client d'agir, faux s'il attend Seheli. */
    public boolean parClient() {
        return parClient;
    }

    public static ProchaineEtape de(StatutDossier statut) {
        return switch (statut) {
            case DEMANDE_SOUMISE, EN_ANALYSE -> ATTENDRE_DECISION;
            case ACCEPTEE -> PAYER_APPORT_ET_SIGNER_AUTORISATION;
            case APPORT_ET_AUTORISATION -> ATTENDRE_COMMANDE;
            case COMMANDEE -> RECEVOIR_ET_CONFIRMER_REMISE;
            case LIVREE -> ATTENDRE_PREMIER_PRELEVEMENT;
            case EN_REMBOURSEMENT -> PROVISIONNER_COMPTE;
            case EN_RETARD -> REGULARISER_ECHEANCE;
            case RECOUVREMENT -> CONTACTER_CIC;
            case REFUSEE, ANNULEE, SOLDEE -> NOUVELLE_DEMANDE_POSSIBLE;
        };
    }
}
