package com.cic.demande.application;

import com.cic.commun.domain.ErreurMetier;

/** Nombre de mensualités hors des bornes en vigueur ({@code duree.min.mois} – {@code duree.max.mois}). */
public class DureeInvalideException extends ErreurMetier {

    public static final String CODE = "demande.duree.invalide";

    public DureeInvalideException() {
        super(CODE, "Durée hors bornes");
    }
}
