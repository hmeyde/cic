package com.cic.inscription.domain;

import com.cic.commun.domain.ErreurMetier;

/** Anti-doublon : une pré-inscription existe déjà pour ce numéro de téléphone. */
public class TelephoneDejaInscritException extends ErreurMetier {

    public static final String CODE = "inscription.telephone.deja-inscrit";

    public TelephoneDejaInscritException() {
        super(CODE, "Inscription refusée : téléphone déjà inscrit");
    }
}
