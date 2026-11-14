package com.seheli.catalogue.application;

import com.seheli.commun.domain.ErreurMetier;

/** Un produit ne peut être rattaché qu'à un fournisseur actif. */
public class FournisseurInactifException extends ErreurMetier {

    public static final String CODE = "bo.produit.fournisseur.inactif";

    public FournisseurInactifException() {
        super(CODE, "Fournisseur inactif");
    }
}
