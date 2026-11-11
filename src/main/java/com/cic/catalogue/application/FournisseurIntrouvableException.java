package com.cic.catalogue.application;

import com.cic.commun.domain.ErreurMetier;

/** Aucun fournisseur ne porte cet identifiant. */
public class FournisseurIntrouvableException extends ErreurMetier {

    public static final String CODE = "bo.fournisseur.introuvable";

    public FournisseurIntrouvableException() {
        super(CODE, "Fournisseur introuvable");
    }
}
