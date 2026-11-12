package com.cic.demande.domain;

import com.cic.commun.domain.ErreurMetier;

/** Un refus exige un motif, d'au plus {@link Demande#LONGUEUR_MOTIF_MAX} caractères. */
public class MotifRefusInvalideException extends ErreurMetier {

    public static final String CODE = "dossier.refus.motif.invalide";

    public MotifRefusInvalideException() {
        super(CODE, "Motif de refus absent ou trop long");
    }
}
