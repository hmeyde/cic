package com.seheli.contrat.domain;

/** Moyen par lequel les mensualités sont prélevées sur le salaire du client. */
public enum TypeAutorisation {
    /** Ordre de virement permanent du compte du client vers le compte de Seheli, signé pour sa banque. */
    VIREMENT_PERMANENT,
    /** Salaire domicilié à la BMCI : prélèvement par la BMCI. */
    DOMICILIATION_BMCI,
    /** Retenue sur salaire par l'employeur. */
    RETENUE_EMPLOYEUR
}
