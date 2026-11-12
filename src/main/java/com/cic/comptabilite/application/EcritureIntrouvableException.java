package com.cic.comptabilite.application;

import com.cic.commun.domain.ErreurMetier;

/** Aucune écriture ne porte cet identifiant. */
public class EcritureIntrouvableException extends ErreurMetier {

    public static final String CODE = "comptabilite.ecriture.introuvable";

    public EcritureIntrouvableException() {
        super(CODE, "Écriture comptable introuvable");
    }
}
