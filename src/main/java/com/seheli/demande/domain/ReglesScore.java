package com.seheli.demande.domain;

import java.util.Objects;

import com.seheli.commun.domain.Fraction;

/**
 * Paramètres de la note, lus dans la table {@code parametre} : {@code score.grille.version},
 * {@code score.seuil.favorable}, {@code score.seuil.a.examiner} et
 * {@code endettement.max.fraction} (borne haute de la tranche « 20 à 33 % » du critère
 * part du salaire).
 */
public record ReglesScore(String versionGrille, int seuilFavorable, int seuilAExaminer, Fraction endettementMax) {

    public ReglesScore {
        Objects.requireNonNull(versionGrille, "versionGrille");
        Objects.requireNonNull(endettementMax, "endettementMax");
        if (seuilAExaminer < 0 || seuilAExaminer > seuilFavorable || seuilFavorable > 100) {
            throw new IllegalArgumentException("Seuils de score incohérents : à examiner " + seuilAExaminer
                    + ", favorable " + seuilFavorable);
        }
    }
}
