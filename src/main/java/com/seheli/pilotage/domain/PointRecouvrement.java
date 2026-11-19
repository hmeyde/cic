package com.seheli.pilotage.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

import com.seheli.commun.domain.Montant;

/**
 * Recouvrement des échéances échues d'un mois : ce qui était attendu (échéances PAYEE ou EN_RETARD
 * dont la date tombe dans le mois et est passée) et ce qui en a été encaissé (PAYEE), à ce jour.
 */
public record PointRecouvrement(YearMonth mois, Montant attendu, Montant encaisse) {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    public PointRecouvrement {
        Objects.requireNonNull(mois, "mois");
        Objects.requireNonNull(attendu, "attendu");
        Objects.requireNonNull(encaisse, "encaisse");
        if (encaisse.estSuperieurA(attendu)) {
            throw new IllegalArgumentException("Encaissé supérieur à l'attendu : " + encaisse + " > " + attendu);
        }
    }

    /** Encaissé / attendu en %, arrondi au dixième (HALF_UP) ; vide si aucune échéance n'était attendue. */
    public Optional<BigDecimal> tauxPct() {
        if (attendu.estZero()) {
            return Optional.empty();
        }
        return Optional.of(encaisse.valeur().multiply(CENT).divide(attendu.valeur(), 1, RoundingMode.HALF_UP));
    }

    public Montant impaye() {
        return attendu.moins(encaisse);
    }
}
