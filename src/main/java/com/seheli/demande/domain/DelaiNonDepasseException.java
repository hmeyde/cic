package com.seheli.demande.domain;

import com.seheli.commun.domain.ErreurMetier;

/** Un dossier ACCEPTEE n'est annulé qu'une fois écoulé le délai de l'apport et de l'autorisation de prélèvement. */
public class DelaiNonDepasseException extends ErreurMetier {

    public static final String CODE = "dossier.delai.non.depasse";

    public DelaiNonDepasseException() {
        super(CODE, "Délai de l'apport et de l'autorisation pas encore écoulé");
    }
}
