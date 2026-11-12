package com.cic.contrat.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Statut de l'autorisation de prélèvement. {@link #A_SIGNER} est l'état initial : l'autorisation est
 * émise avec le contrat et attend la signature du client.
 *
 * <ul>
 *   <li>A_SIGNER → SIGNEE : le document signé par le client est déposé au back-office ;</li>
 *   <li>SIGNEE → TRANSMISE : remise à la banque (ou à l'employeur) ;</li>
 *   <li>TRANSMISE → ACCEPTEE : la banque a visé l'ordre ;</li>
 *   <li>ACCEPTEE → ACTIVE : premier prélèvement fait ;</li>
 *   <li>SIGNEE, TRANSMISE, ACCEPTEE, ACTIVE → REVOQUEE (état final).</li>
 * </ul>
 * Toute autre transition est interdite.
 */
public enum StatutAutorisation {
    A_SIGNER,
    SIGNEE,
    TRANSMISE,
    ACCEPTEE,
    ACTIVE,
    REVOQUEE;

    /** Statuts atteignables depuis celui-ci. */
    public Set<StatutAutorisation> suivants() {
        return switch (this) {
            case A_SIGNER -> EnumSet.of(SIGNEE);
            case SIGNEE -> EnumSet.of(TRANSMISE, REVOQUEE);
            case TRANSMISE -> EnumSet.of(ACCEPTEE, REVOQUEE);
            case ACCEPTEE -> EnumSet.of(ACTIVE, REVOQUEE);
            case ACTIVE -> EnumSet.of(REVOQUEE);
            case REVOQUEE -> EnumSet.noneOf(StatutAutorisation.class);
        };
    }

    public boolean peutPasserA(StatutAutorisation vers) {
        return suivants().contains(vers);
    }

    /** Le document signé peut être déposé (première fois) ou remplacé (scan illisible) tant que l'ordre n'est pas parti à la banque. */
    public boolean accepteDocumentSigne() {
        return this == A_SIGNER || this == SIGNEE;
    }
}
