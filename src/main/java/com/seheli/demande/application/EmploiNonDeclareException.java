package com.seheli.demande.application;

import com.seheli.commun.domain.ErreurMetier;

/** Le client n'a pas encore déclaré son emploi : ni salaire, ni jour de paie, ni contrat à étudier. */
public class EmploiNonDeclareException extends ErreurMetier {

    public static final String CODE = "demande.emploi.manquant";

    public EmploiNonDeclareException() {
        super(CODE, "Emploi du client non déclaré");
    }
}
