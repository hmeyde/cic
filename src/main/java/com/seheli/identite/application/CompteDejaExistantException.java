package com.seheli.identite.application;

import com.seheli.commun.domain.ErreurMetier;

/** Un compte existe déjà pour ce téléphone. */
public class CompteDejaExistantException extends ErreurMetier {

    public CompteDejaExistantException() {
        super("client.compte.deja.existant", "Compte déjà existant");
    }
}
