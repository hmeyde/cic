package com.cic.paiement.application;

import com.cic.commun.domain.ErreurMetier;

/** Fichier CSV de paiements refusé en entier : rien n'est importé. */
public class FichierCsvInvalideException extends ErreurMetier {

    public static final String VIDE = "bo.import.fichier.vide";
    public static final String TROP_GROS = "bo.import.fichier.trop-gros";
    public static final String TROP_DE_LIGNES = "bo.import.fichier.trop-de-lignes";

    public FichierCsvInvalideException(String code) {
        super(code, "Fichier CSV refusé : " + code);
    }
}
