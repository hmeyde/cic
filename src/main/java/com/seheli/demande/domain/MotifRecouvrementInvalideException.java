package com.seheli.demande.domain;

import com.seheli.commun.domain.ErreurMetier;

/** Le passage en recouvrement exige un motif, d'au plus {@link Demande#LONGUEUR_MOTIF_MAX} caractères. */
public class MotifRecouvrementInvalideException extends ErreurMetier {

    public static final String CODE = "dossier.recouvrement.motif.invalide";

    public MotifRecouvrementInvalideException() {
        super(CODE, "Motif de passage en recouvrement absent ou trop long");
    }
}
