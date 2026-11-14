package com.seheli.identite.application;

import com.seheli.commun.domain.ErreurMetier;

/** L'emploi déclaré n'a pas encore commencé : seul l'emploi actuel est enregistré. */
public class DateDebutEmploiFutureException extends ErreurMetier {

    public DateDebutEmploiFutureException() {
        super("client.emploi.date-debut.future", "Date de début d'emploi dans le futur");
    }
}
