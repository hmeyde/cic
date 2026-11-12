package com.cic.contrat.application;

/** Aucune autorisation de prélèvement sous cet identifiant. */
public class AutorisationIntrouvableException extends RuntimeException {

    public AutorisationIntrouvableException() {
        super("Autorisation de prélèvement introuvable");
    }
}
