package com.cic.demande.domain;

/**
 * Mode de remboursement choisi par le client, critère « mode de remboursement » du score.
 * Mêmes valeurs que le type d'autorisation de prélèvement.
 */
public enum ModeRemboursement {
    RETENUE_EMPLOYEUR,
    DOMICILIATION_BMCI,
    VIREMENT_PERMANENT
}
