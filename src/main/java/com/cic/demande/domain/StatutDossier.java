package com.cic.demande.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Statut d'un dossier et machine à états de son cycle de vie (CLAUDE.md, « Cycle de vie d'un dossier »).
 *
 * <pre>
 * DEMANDE_SOUMISE → EN_ANALYSE → ACCEPTEE | REFUSEE
 * ACCEPTEE → APPORT_ET_AUTORISATION | ANNULEE
 * APPORT_ET_AUTORISATION → COMMANDEE → LIVREE → EN_REMBOURSEMENT
 * EN_REMBOURSEMENT → SOLDEE | EN_RETARD
 * EN_RETARD → EN_REMBOURSEMENT | RECOUVREMENT
 * </pre>
 *
 * Toute autre transition lève {@link TransitionInterditeException}.
 */
public enum StatutDossier {
    DEMANDE_SOUMISE,
    EN_ANALYSE,
    ACCEPTEE,
    REFUSEE,
    ANNULEE,
    APPORT_ET_AUTORISATION,
    COMMANDEE,
    LIVREE,
    EN_REMBOURSEMENT,
    EN_RETARD,
    SOLDEE,
    RECOUVREMENT;

    private static final Map<StatutDossier, Set<StatutDossier>> TRANSITIONS = new EnumMap<>(StatutDossier.class);

    static {
        autoriser(DEMANDE_SOUMISE, EN_ANALYSE);
        autoriser(EN_ANALYSE, ACCEPTEE, REFUSEE);
        autoriser(ACCEPTEE, APPORT_ET_AUTORISATION, ANNULEE);
        autoriser(APPORT_ET_AUTORISATION, COMMANDEE);
        autoriser(COMMANDEE, LIVREE);
        autoriser(LIVREE, EN_REMBOURSEMENT);
        autoriser(EN_REMBOURSEMENT, SOLDEE, EN_RETARD);
        autoriser(EN_RETARD, EN_REMBOURSEMENT, RECOUVREMENT);
        for (StatutDossier statut : values()) {
            TRANSITIONS.putIfAbsent(statut, Collections.unmodifiableSet(EnumSet.noneOf(StatutDossier.class)));
        }
    }

    private static void autoriser(StatutDossier depuis, StatutDossier premier, StatutDossier... autres) {
        TRANSITIONS.put(depuis, Collections.unmodifiableSet(EnumSet.of(premier, autres)));
    }

    /** Statuts atteignables directement depuis celui-ci (vide pour un statut final). */
    public Set<StatutDossier> suivants() {
        return TRANSITIONS.get(this);
    }

    public boolean peutPasserA(StatutDossier cible) {
        Objects.requireNonNull(cible, "cible");
        return suivants().contains(cible);
    }

    /** Statut sans suivant : REFUSEE, ANNULEE, SOLDEE, RECOUVREMENT. */
    public boolean estFinal() {
        return suivants().isEmpty();
    }

    /**
     * Vérifie la transition et la décrit pour le journal d'audit.
     *
     * @throws TransitionInterditeException si la transition n'est pas permise
     */
    public ChangementStatut passerA(StatutDossier cible) {
        if (!peutPasserA(cible)) {
            throw new TransitionInterditeException(this, cible);
        }
        return new ChangementStatut(this, cible);
    }
}
