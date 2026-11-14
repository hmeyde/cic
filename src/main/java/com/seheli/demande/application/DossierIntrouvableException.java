package com.seheli.demande.application;

import com.seheli.commun.domain.ErreurMetier;

/** Aucun dossier ne porte cet identifiant. */
public class DossierIntrouvableException extends ErreurMetier {

    public static final String CODE = "dossier.introuvable";

    public DossierIntrouvableException() {
        super(CODE, "Dossier introuvable");
    }
}
