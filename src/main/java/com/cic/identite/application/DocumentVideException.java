package com.cic.identite.application;

import com.cic.commun.domain.ErreurMetier;

/** Aucun fichier, ou un fichier vide. */
public class DocumentVideException extends ErreurMetier {

    public DocumentVideException() {
        super("client.justificatifs.fichier.obligatoire", "Fichier vide ou absent");
    }
}
