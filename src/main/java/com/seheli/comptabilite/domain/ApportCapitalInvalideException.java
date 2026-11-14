package com.seheli.comptabilite.domain;

import com.seheli.commun.domain.ErreurMetier;

/** Saisie d'un apport en capital refusée : associé, montant ou date du versement. */
public class ApportCapitalInvalideException extends ErreurMetier {

    public static final String ASSOCIE_INVALIDE = "bo.capital.associe.invalide";
    public static final String MONTANT_INVALIDE = "bo.capital.montant.invalide";
    public static final String DATE_INVALIDE = "bo.capital.date.invalide";

    public ApportCapitalInvalideException(String code) {
        super(code, "Apport en capital refusé : " + code);
    }
}
