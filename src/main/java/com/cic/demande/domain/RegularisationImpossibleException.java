package com.cic.demande.domain;

import com.cic.commun.domain.ErreurMetier;

/** Un dossier EN_RETARD n'est régularisé qu'une fois toutes ses échéances en retard payées. */
public class RegularisationImpossibleException extends ErreurMetier {

    public static final String CODE = "dossier.regularisation.impossible";

    public RegularisationImpossibleException() {
        super(CODE, "Une échéance est encore en retard");
    }
}
