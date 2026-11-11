package com.cic.identite.domain;

/**
 * Rôles de l'équipe CIC dans le back-office. Le rôle CLIENT n'en fait pas partie :
 * les clients se connectent par code à usage unique, hors du back-office.
 */
public enum RoleBackOffice {
    ANALYSTE,
    RECOUVREMENT,
    ADMIN,
    ASSOCIE
}
