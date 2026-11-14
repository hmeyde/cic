package com.seheli.contrat.domain;

import com.seheli.commun.domain.ErreurMetier;

/** Transition de l'autorisation de prélèvement non prévue par les règles de gestion. */
public class TransitionAutorisationInterditeException extends ErreurMetier {

    public static final String CODE = "autorisation.transition.interdite";

    private final StatutAutorisation depuis;
    private final StatutAutorisation vers;

    public TransitionAutorisationInterditeException(StatutAutorisation depuis, StatutAutorisation vers) {
        super(CODE, "Transition d'autorisation interdite : " + depuis + " → " + vers);
        this.depuis = depuis;
        this.vers = vers;
    }

    public StatutAutorisation depuis() {
        return depuis;
    }

    public StatutAutorisation vers() {
        return vers;
    }
}
