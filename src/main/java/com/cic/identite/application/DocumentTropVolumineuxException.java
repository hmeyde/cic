package com.cic.identite.application;

import com.cic.commun.domain.ErreurMetier;

/** Le fichier dépasse la taille maximale d'un justificatif (5 Mo). */
public class DocumentTropVolumineuxException extends ErreurMetier {

    public DocumentTropVolumineuxException() {
        super("client.justificatifs.fichier.trop-volumineux", "Fichier trop volumineux");
    }
}
