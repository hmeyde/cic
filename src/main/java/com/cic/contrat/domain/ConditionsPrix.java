package com.cic.contrat.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Taux appliqués au calcul du prix, en pourcentage ({@code 8} pour 8 %).
 *
 * @param remiseFournisseurPct remise obtenue du fournisseur, en % du prix public
 * @param margeCicPct          marge CIC, en % du prix public (fixée par catégorie de produit)
 * @param apportMinimumPct     apport minimum, en % du prix de vente
 */
public record ConditionsPrix(BigDecimal remiseFournisseurPct, BigDecimal margeCicPct, BigDecimal apportMinimumPct) {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    public ConditionsPrix {
        Objects.requireNonNull(remiseFournisseurPct, "remiseFournisseurPct");
        Objects.requireNonNull(margeCicPct, "margeCicPct");
        Objects.requireNonNull(apportMinimumPct, "apportMinimumPct");
        exigerEntre(remiseFournisseurPct, "Remise fournisseur", false);
        exigerEntre(apportMinimumPct, "Apport minimum", true);
        if (margeCicPct.signum() < 0) {
            throw new IllegalArgumentException("Marge CIC négative : " + margeCicPct + " %");
        }
    }

    private static void exigerEntre(BigDecimal taux, String libelle, boolean centAutorise) {
        boolean tropGrand = centAutorise ? taux.compareTo(CENT) > 0 : taux.compareTo(CENT) >= 0;
        if (taux.signum() < 0 || tropGrand) {
            throw new IllegalArgumentException(libelle + " hors bornes : " + taux + " %");
        }
    }
}
