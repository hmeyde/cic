package com.cic.demande.domain;

import java.util.Objects;

/** Lecture de la note : le score recommande, l'analyste décide toujours. */
public enum Recommandation {
    FAVORABLE,
    A_EXAMINER,
    DEFAVORABLE;

    /** Note ≥ seuil favorable (70) : FAVORABLE ; ≥ seuil à examiner (50) : A_EXAMINER ; sinon DEFAVORABLE. */
    public static Recommandation selon(int note, ReglesScore regles) {
        Objects.requireNonNull(regles, "regles");
        if (note >= regles.seuilFavorable()) {
            return FAVORABLE;
        }
        if (note >= regles.seuilAExaminer()) {
            return A_EXAMINER;
        }
        return DEFAVORABLE;
    }
}
