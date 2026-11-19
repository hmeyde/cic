package com.seheli.demande.domain;

/** Avancement d'un jalon du parcours pour un dossier donné. */
public enum EtatEtape {
    FAITE,
    /** Jalon courant : le dernier atteint, mis en avant dans la frise. */
    EN_COURS,
    /** Jalon courant d'un dossier en difficulté : échéance en retard ou recouvrement. */
    EN_ALERTE,
    /** Le dossier s'est arrêté avant ce jalon (REFUSEE, ANNULEE). */
    ARRETEE,
    A_VENIR
}
