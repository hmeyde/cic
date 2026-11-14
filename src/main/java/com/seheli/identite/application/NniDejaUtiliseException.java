package com.seheli.identite.application;

import com.seheli.commun.domain.ErreurMetier;

/** Un client porte déjà ce NNI : un NNI = un client. */
public class NniDejaUtiliseException extends ErreurMetier {

    public NniDejaUtiliseException() {
        super("client.compte.nni.deja.utilise", "NNI déjà utilisé");
    }
}
