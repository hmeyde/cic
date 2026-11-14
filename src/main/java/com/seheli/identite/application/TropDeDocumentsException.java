package com.seheli.identite.application;

import com.seheli.commun.domain.ErreurMetier;

/** Le client a atteint le nombre maximal de documents de ce type. */
public class TropDeDocumentsException extends ErreurMetier {

    public TropDeDocumentsException() {
        super("client.justificatifs.trop-nombreux", "Trop de documents de ce type");
    }
}
