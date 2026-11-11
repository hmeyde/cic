package com.cic.identite.application;

import com.cic.commun.domain.ErreurMetier;

/** Le CDD déclaré est déjà terminé : seul l'emploi actuel est enregistré. */
public class ContratTermineException extends ErreurMetier {

    public ContratTermineException() {
        super("client.emploi.date-fin.passee", "Contrat déjà terminé");
    }
}
