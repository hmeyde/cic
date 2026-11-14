package com.seheli.parametrage.domain;

import java.math.BigDecimal;

import com.seheli.commun.domain.Fraction;

/** Type d'une valeur de paramètre ; chaque type sait vérifier qu'une valeur texte lui est conforme. */
public enum TypeParametre {

    /** Entier positif ou nul, par exemple une durée en mois ou un nombre de jours. */
    ENTIER {
        @Override
        void verifier(String valeur) {
            if (Integer.parseInt(valeur) < 0) {
                throw new IllegalArgumentException("entier négatif");
            }
        }
    },
    /** Pourcentage décimal entre 0 et 100, par exemple {@code 40} ou {@code 7.5}. */
    POURCENTAGE {
        @Override
        void verifier(String valeur) {
            BigDecimal taux = new BigDecimal(valeur);
            if (taux.signum() < 0 || taux.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("pourcentage hors de [0, 100]");
            }
        }
    },
    /** Fraction exacte {@code a/b}, par exemple {@code 1/3}. */
    FRACTION {
        @Override
        void verifier(String valeur) {
            Fraction.de(valeur);
        }
    },
    /** Texte libre non vide. */
    TEXTE {
        @Override
        void verifier(String valeur) {
            // toute valeur non vide convient
        }
    };

    /** @throws IllegalArgumentException si la valeur n'est pas conforme au type */
    abstract void verifier(String valeur);
}
