package com.cic.demande.domain;

/** Avancement d'une étape du parcours pour un dossier donné. */
public enum EtatEtape {
    FAITE,
    EN_COURS,
    /** Le dossier s'est arrêté à cette étape (REFUSEE, ANNULEE). */
    ARRETEE,
    A_VENIR
}
