package com.cic.demande.application;

import com.cic.commun.domain.ErreurMetier;

/** Le client n'a pas coché la case attestant qu'il a reçu le produit. */
public class RemiseNonConfirmeeException extends ErreurMetier {

    public static final String CODE = "dossier.remise.non.confirmee";

    public RemiseNonConfirmeeException() {
        super(CODE, "La remise du produit n'est pas confirmée par le client");
    }
}
