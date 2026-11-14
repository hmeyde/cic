package com.seheli.contrat.application;

import com.seheli.commun.domain.ErreurMetier;

/** Saisie du back-office refusée lors du suivi d'une autorisation : document signé ou motif de révocation. */
public class SaisieAutorisationInvalideException extends ErreurMetier {

    public static final String DOCUMENT_VIDE = "bo.autorisation.document.vide";
    public static final String DOCUMENT_TROP_VOLUMINEUX = "bo.autorisation.document.trop-volumineux";
    public static final String FORMAT_REFUSE = "bo.autorisation.document.format-refuse";
    public static final String MOTIF_INVALIDE = "bo.autorisation.revocation.motif-invalide";

    public SaisieAutorisationInvalideException(String code) {
        super(code, "Saisie refusée : " + code);
    }
}
