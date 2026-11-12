package com.cic.demande.application;

import com.cic.commun.domain.ErreurMetier;

/** Apport proposé inférieur à l'apport minimum, ou couvrant tout le prix de vente (rien à financer). */
public class ApportInvalideException extends ErreurMetier {

    public static final String CODE = "demande.apport.invalide";

    public ApportInvalideException() {
        super(CODE, "Apport hors bornes");
    }
}
