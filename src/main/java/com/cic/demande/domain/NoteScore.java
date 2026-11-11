package com.cic.demande.domain;

import java.util.List;
import java.util.Objects;

/**
 * Note sur 100 : points et raison de chaque critère dans l'ordre de la grille, version de la
 * grille appliquée et recommandation tirée de la note seule.
 */
public record NoteScore(String versionGrille, List<ResultatCritere> criteres, Recommandation recommandation) {

    public NoteScore {
        Objects.requireNonNull(versionGrille, "versionGrille");
        Objects.requireNonNull(criteres, "criteres");
        Objects.requireNonNull(recommandation, "recommandation");
        criteres = List.copyOf(criteres);
    }

    /** Somme des points de tous les critères, de 0 à 100. */
    public int note() {
        return criteres.stream().mapToInt(ResultatCritere::points).sum();
    }

    public ResultatCritere resultat(CritereScore critere) {
        return criteres.stream().filter(r -> r.critere() == critere).findFirst().orElseThrow();
    }
}
