package com.cic.demande.domain;

/**
 * Conditions éliminatoires du score v1. Une seule condition non satisfaite rend le dossier
 * inéligible, quelle que soit la note. Le code sert de clé de message i18n.
 */
public enum ConditionEliminatoire {

    IDENTITE_VERIFIEE("score.eliminatoire.identite"),
    AGE_MINIMUM("score.eliminatoire.age"),
    CONTRAT_EN_COURS("score.eliminatoire.contrat"),
    SALAIRE_SUR_COMPTE("score.eliminatoire.salaire.compte"),
    ENDETTEMENT("score.eliminatoire.endettement"),
    AUCUN_RETARD("score.eliminatoire.retard");

    private final String code;

    ConditionEliminatoire(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
