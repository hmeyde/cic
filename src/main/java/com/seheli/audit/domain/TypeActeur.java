package com.seheli.audit.domain;

/** Origine d'une action journalisée. */
public enum TypeActeur {
    /** Le client lui-même, depuis son espace. */
    CLIENT,
    /** Un membre de l'équipe Seheli, depuis le back-office. */
    BACK_OFFICE,
    /** Un traitement automatique (job planifié, événement). */
    SYSTEME
}
