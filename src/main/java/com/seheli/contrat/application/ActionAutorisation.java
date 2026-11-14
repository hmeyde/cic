package com.seheli.contrat.application;

/**
 * Actions du suivi d'une autorisation de prélèvement consignées au journal d'audit (code stable,
 * clé de message {@code bo.autorisation.journal.action.*}).
 */
public enum ActionAutorisation {
    DEPOT_DOCUMENT_SIGNE,
    REMPLACEMENT_DOCUMENT_SIGNE,
    TRANSMISSION,
    ACCEPTATION,
    ACTIVATION,
    REVOCATION
}
