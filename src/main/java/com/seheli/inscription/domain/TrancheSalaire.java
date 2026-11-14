package com.seheli.inscription.domain;

/**
 * Tranche de salaire net mensuel déclarée lors de la pré-inscription, en MRU. Une tranche suffit à
 * estimer l'intérêt du prospect : le salaire exact n'est demandé qu'à la création du compte client.
 */
public enum TrancheSalaire {
    /** Moins de 10 000 MRU. */
    MOINS_DE_10000,
    /** De 10 000 à 20 000 MRU. */
    DE_10000_A_20000,
    /** De 20 000 à 40 000 MRU. */
    DE_20000_A_40000,
    /** Plus de 40 000 MRU. */
    PLUS_DE_40000
}
