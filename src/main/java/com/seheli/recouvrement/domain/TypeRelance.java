package com.seheli.recouvrement.domain;

/** Motif d'une relance envoyée au client. */
public enum TypeRelance {
    /** Rappel envoyé avant chaque prélèvement ({@code relance.avant.prelevement.jours}). */
    AVANT_PRELEVEMENT,
    /** Première relance après une échéance impayée ({@code relance.echec.premiere.jours}, J+3). */
    ECHEC_PREMIERE,
    /** Seconde relance après une échéance impayée ({@code relance.echec.seconde.jours}, J+7). */
    ECHEC_SECONDE
}
