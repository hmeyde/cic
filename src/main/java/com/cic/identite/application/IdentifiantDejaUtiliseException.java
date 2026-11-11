package com.cic.identite.application;

import com.cic.commun.domain.ErreurMetier;

/** Un compte du back-office porte déjà cet identifiant. */
public class IdentifiantDejaUtiliseException extends ErreurMetier {

    public IdentifiantDejaUtiliseException() {
        super("bo.compte.identifiant.deja.utilise", "Identifiant déjà utilisé");
    }
}
