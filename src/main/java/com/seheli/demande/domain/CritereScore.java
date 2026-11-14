package com.seheli.demande.domain;

/**
 * Critères de la note sur 100 de la grille v1, avec leur barème maximal. La somme des
 * maximums vaut 100. Le code sert de clé de message i18n.
 */
public enum CritereScore {

    TYPE_CONTRAT("score.critere.type.contrat", 25),
    ANCIENNETE("score.critere.anciennete", 15),
    MODE_REMBOURSEMENT("score.critere.mode.remboursement", 15),
    PART_SALAIRE("score.critere.part.salaire", 15),
    APPORT_SUPPLEMENTAIRE("score.critere.apport", 10),
    GARANT("score.critere.garant", 10),
    HISTORIQUE_CIC("score.critere.historique", 10);

    private final String code;
    private final int pointsMax;

    CritereScore(String code, int pointsMax) {
        this.code = code;
        this.pointsMax = pointsMax;
    }

    public String code() {
        return code;
    }

    public int pointsMax() {
        return pointsMax;
    }
}
