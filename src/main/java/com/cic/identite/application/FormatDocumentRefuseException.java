package com.cic.identite.application;

import com.cic.commun.domain.ErreurMetier;

/** Le fichier n'est ni un PDF, ni un JPEG, ni un PNG (reconnu à son contenu). */
public class FormatDocumentRefuseException extends ErreurMetier {

    public FormatDocumentRefuseException() {
        super("client.justificatifs.format.refuse", "Format refusé");
    }
}
