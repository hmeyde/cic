package com.cic.identite.application;

import com.cic.commun.domain.ErreurMetier;

/** Le numéro saisi n'est pas un téléphone mauritanien à 8 chiffres. */
public class TelephoneInvalideException extends ErreurMetier {

    public TelephoneInvalideException() {
        super("client.connexion.telephone.invalide", "Téléphone invalide");
    }
}
