package com.seheli.demande.application;

import com.seheli.commun.domain.ErreurMetier;

/** Le client a déjà un dossier actif (paramètre {@code dossier.actif.max}). */
public class DossierActifExistantException extends ErreurMetier {

    public static final String CODE = "demande.dossier.actif";

    public DossierActifExistantException() {
        super(CODE, "Le client a déjà un dossier actif");
    }
}
