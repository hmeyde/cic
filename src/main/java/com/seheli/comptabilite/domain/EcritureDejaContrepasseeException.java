package com.seheli.comptabilite.domain;

import com.seheli.commun.domain.ErreurMetier;

/** Une écriture n'est contrepassée qu'une fois, et une contrepassation ne se contrepasse pas. */
public class EcritureDejaContrepasseeException extends ErreurMetier {

    public static final String CODE = "comptabilite.contrepassation.impossible";

    public EcritureDejaContrepasseeException() {
        super(CODE, "Écriture déjà contrepassée, ou elle-même contrepassation");
    }
}
