package com.cic.identite.domain;

/**
 * Mode de remboursement choisi par le client avec son emploi. Mêmes valeurs que le type
 * d'autorisation de prélèvement ; propre au module identite pour qu'il ne dépende pas de demande.
 */
public enum ModeRemboursement {
    RETENUE_EMPLOYEUR,
    DOMICILIATION_BMCI,
    VIREMENT_PERMANENT
}
