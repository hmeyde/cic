package com.cic.identite.application;

import com.cic.commun.domain.ErreurMetier;

/** Le client n'a pas donné son consentement explicite à l'usage de ses données. */
public class ConsentementManquantException extends ErreurMetier {

    public ConsentementManquantException() {
        super("client.compte.consentement.manquant", "Consentement manquant");
    }
}
