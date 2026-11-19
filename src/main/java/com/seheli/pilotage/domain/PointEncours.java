package com.seheli.pilotage.domain;

import java.time.YearMonth;
import java.util.Objects;

import com.seheli.commun.domain.Montant;

/**
 * Encours à la fin d'un mois (au jour du calcul pour le mois en cours).
 *
 * @param mois    mois du point
 * @param encours reste dû par les clients sur les dossiers dont le fournisseur est payé
 */
public record PointEncours(YearMonth mois, Montant encours) {

    public PointEncours {
        Objects.requireNonNull(mois, "mois");
        Objects.requireNonNull(encours, "encours");
    }
}
