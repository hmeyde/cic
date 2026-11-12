package com.cic.contrat.domain;

/**
 * Statut de l'autorisation de prélèvement. {@link #A_SIGNER} est l'état initial : l'autorisation est
 * émise avec le contrat et attend la signature du client ; viennent ensuite SIGNEE, TRANSMISE (à la
 * banque ou à l'employeur), ACCEPTEE, ACTIVE (premier prélèvement fait) et REVOQUEE.
 */
public enum StatutAutorisation {
    A_SIGNER,
    SIGNEE,
    TRANSMISE,
    ACCEPTEE,
    ACTIVE,
    REVOQUEE
}
