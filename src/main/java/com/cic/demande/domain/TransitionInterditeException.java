package com.cic.demande.domain;

import com.cic.commun.domain.ErreurMetier;

/** Transition du cycle de vie d'un dossier non prévue par les règles de gestion. */
public class TransitionInterditeException extends ErreurMetier {

    public static final String CODE = "dossier.transition.interdite";

    private final StatutDossier depuis;
    private final StatutDossier vers;

    public TransitionInterditeException(StatutDossier depuis, StatutDossier vers) {
        super(CODE, "Transition interdite : " + depuis + " → " + vers);
        this.depuis = depuis;
        this.vers = vers;
    }

    public StatutDossier depuis() {
        return depuis;
    }

    public StatutDossier vers() {
        return vers;
    }
}
