package com.cic.paiement.application;

import com.cic.commun.domain.ErreurMetier;

/** Saisie manuelle d'un paiement refusée : référence, montant ou date du paiement. */
public class SaisiePaiementInvalideException extends ErreurMetier {

    public static final String REFERENCE_INVALIDE = "bo.paiement.reference.invalide";
    public static final String MONTANT_INVALIDE = "bo.paiement.montant.invalide";
    public static final String DATE_INVALIDE = "bo.paiement.date.invalide";

    public SaisiePaiementInvalideException(String code) {
        super(code, "Saisie refusée : " + code);
    }
}
