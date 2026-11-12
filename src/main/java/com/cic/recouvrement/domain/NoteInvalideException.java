package com.cic.recouvrement.domain;

import com.cic.commun.domain.ErreurMetier;

/** Une note de recouvrement a un texte, d'au plus {@link NoteRecouvrement#LONGUEUR_MAX} caractères. */
public class NoteInvalideException extends ErreurMetier {

    public static final String CODE = "recouvrement.note.invalide";

    public NoteInvalideException() {
        super(CODE, "Note vide ou trop longue");
    }
}
