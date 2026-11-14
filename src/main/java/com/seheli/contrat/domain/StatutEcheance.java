package com.seheli.contrat.domain;

/** Statut d'une échéance : A_VENIR à la signature, PAYEE une fois prélevée, EN_RETARD après un échec. */
public enum StatutEcheance {
    A_VENIR,
    PAYEE,
    EN_RETARD
}
