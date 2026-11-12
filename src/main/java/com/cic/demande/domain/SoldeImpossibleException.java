package com.cic.demande.domain;

import com.cic.commun.domain.ErreurMetier;

/** Un dossier n'est soldé qu'une fois toutes ses échéances payées. */
public class SoldeImpossibleException extends ErreurMetier {

    public static final String CODE = "dossier.solde.impossible";

    public SoldeImpossibleException() {
        super(CODE, "Des échéances restent à payer");
    }
}
