package com.cic.catalogue.application;

import com.cic.commun.domain.ErreurMetier;

/** Aucun produit ne porte cet identifiant. */
public class ProduitIntrouvableException extends ErreurMetier {

    public static final String CODE = "bo.produit.introuvable";

    public ProduitIntrouvableException() {
        super(CODE, "Produit introuvable");
    }
}
