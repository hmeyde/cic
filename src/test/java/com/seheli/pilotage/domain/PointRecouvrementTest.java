package com.seheli.pilotage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;

/** Taux de recouvrement des échéances échues d'un mois. */
class PointRecouvrementTest {

    private static final YearMonth NOVEMBRE = YearMonth.of(2026, 11);

    @Test
    void le_taux_est_l_encaisse_rapporte_a_l_attendu_arrondi_au_dixieme() {
        PointRecouvrement p = new PointRecouvrement(NOVEMBRE, Montant.de(6_240), Montant.de(2_080));

        assertThat(p.tauxPct()).contains(new BigDecimal("33.3"));
        assertThat(p.impaye()).isEqualTo(Montant.de(4_160));
    }

    @Test
    void sans_echeance_attendue_il_n_y_a_pas_de_taux() {
        assertThat(new PointRecouvrement(NOVEMBRE, Montant.ZERO, Montant.ZERO).tauxPct()).isEmpty();
    }

    @Test
    void l_encaisse_ne_peut_pas_depasser_l_attendu() {
        assertThatThrownBy(() -> new PointRecouvrement(NOVEMBRE, Montant.de(2_080), Montant.de(4_160)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
