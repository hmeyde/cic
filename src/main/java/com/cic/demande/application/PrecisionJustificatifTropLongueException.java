package com.cic.demande.application;

import com.cic.commun.domain.ErreurMetier;

/** La précision jointe à une demande de justificatif dépasse {@link DecisionService#LONGUEUR_PRECISION_MAX} caractères. */
public class PrecisionJustificatifTropLongueException extends ErreurMetier {

    public static final String CODE = "dossier.justificatif.precision.longue";

    public PrecisionJustificatifTropLongueException() {
        super(CODE, "Précision du justificatif demandé trop longue");
    }
}
