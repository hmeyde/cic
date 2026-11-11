package com.cic.identite.application;

import com.cic.commun.domain.ErreurMetier;

/** La date de naissance est dans le futur ou invraisemblable. */
public class DateNaissanceInvalideException extends ErreurMetier {

    public DateNaissanceInvalideException() {
        super("client.compte.date-naissance.invalide", "Date de naissance invalide");
    }
}
