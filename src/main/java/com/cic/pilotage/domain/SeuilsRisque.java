package com.cic.pilotage.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Seuils de risque lus dans la table {@code parametre} (pourcentages tels que stockés : 20 pour 20 %).
 *
 * @param provisionPalier1Jours        retard à partir duquel le restant dû est provisionné au premier taux
 * @param provisionPalier1Pct          premier taux de provision (50 %)
 * @param provisionPalier2Jours        retard à partir duquel s'applique le second taux
 * @param provisionPalier2Pct          second taux de provision (100 %)
 * @param perteJours                   retard à partir duquel le restant dû passe en perte
 * @param employeurMaxPct              part maximale de l'encours sur un même employeur
 * @param fournisseurMaxPct            part maximale de l'encours sur un même fournisseur
 * @param encoursCapitalMaxPct         encours maximal en % du capital disponible
 * @param prelevementsReussisMinPct    part minimale d'échéances échues payées
 */
public record SeuilsRisque(int provisionPalier1Jours, BigDecimal provisionPalier1Pct, int provisionPalier2Jours,
        BigDecimal provisionPalier2Pct, int perteJours, BigDecimal employeurMaxPct, BigDecimal fournisseurMaxPct,
        BigDecimal encoursCapitalMaxPct, BigDecimal prelevementsReussisMinPct) {

    public SeuilsRisque {
        Objects.requireNonNull(provisionPalier1Pct, "provisionPalier1Pct");
        Objects.requireNonNull(provisionPalier2Pct, "provisionPalier2Pct");
        Objects.requireNonNull(employeurMaxPct, "employeurMaxPct");
        Objects.requireNonNull(fournisseurMaxPct, "fournisseurMaxPct");
        Objects.requireNonNull(encoursCapitalMaxPct, "encoursCapitalMaxPct");
        Objects.requireNonNull(prelevementsReussisMinPct, "prelevementsReussisMinPct");
        if (provisionPalier1Jours <= 0 || provisionPalier2Jours < provisionPalier1Jours
                || perteJours < provisionPalier2Jours) {
            throw new IllegalArgumentException("Paliers de retard incohérents : " + provisionPalier1Jours + ", "
                    + provisionPalier2Jours + ", " + perteJours);
        }
    }
}
