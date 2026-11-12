package com.cic.demande.domain;

/**
 * Conditions à réunir avant de commander le produit et de payer le fournisseur : l'apport est
 * encaissé (total reçu au moins égal à l'apport fixé au contrat) et l'autorisation de prélèvement
 * est ACCEPTEE par la banque (ou déjà ACTIVE). Établies par les modules paiement et contrat.
 */
public record PrealablesCommande(boolean apportEncaisse, boolean autorisationAcceptee) {

    public static final PrealablesCommande AUCUN = new PrealablesCommande(false, false);

    public boolean sontReunis() {
        return apportEncaisse && autorisationAcceptee;
    }
}
