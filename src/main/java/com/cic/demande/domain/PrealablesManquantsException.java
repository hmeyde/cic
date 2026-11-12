package com.cic.demande.domain;

import com.cic.commun.domain.ErreurMetier;

/** Le produit n'est jamais commandé ni le fournisseur payé sans apport encaissé ET autorisation de prélèvement ACCEPTEE. */
public class PrealablesManquantsException extends ErreurMetier {

    public static final String CODE = "dossier.prealables.manquants";

    public PrealablesManquantsException(PrealablesCommande prealables) {
        super(CODE, "Apport encaissé : " + prealables.apportEncaisse() + ", autorisation acceptée : "
                + prealables.autorisationAcceptee());
    }
}
