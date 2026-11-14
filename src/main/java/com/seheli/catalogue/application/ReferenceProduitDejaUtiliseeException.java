package com.seheli.catalogue.application;

import com.seheli.commun.domain.ErreurMetier;

/** Un autre produit porte déjà cette référence. */
public class ReferenceProduitDejaUtiliseeException extends ErreurMetier {

    public static final String CODE = "bo.produit.reference.deja.utilisee";

    public ReferenceProduitDejaUtiliseeException() {
        super(CODE, "Référence produit déjà utilisée");
    }
}
