package com.cic.commun.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FractionTest {

    @Test
    void lit_le_format_a_sur_b() {
        assertThat(Fraction.de(" 1 / 3 ")).isEqualTo(new Fraction(1, 3));
        assertThat(Fraction.de("1/3")).hasToString("1/3");
    }

    @Test
    void refuse_les_formats_invalides() {
        assertThatThrownBy(() -> Fraction.de("0.33")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Fraction.de("1/0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Fraction.de("-1/3")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_tiers_est_compare_exactement_sans_arrondi() {
        Fraction tiers = new Fraction(1, 3);
        Montant salaire = Montant.de(30_000);

        assertThat(tiers.admet(Montant.de(10_000), salaire)).isTrue();
        assertThat(tiers.admet(Montant.de(10_001), salaire)).isFalse();
        // 2 080 MRU de mensualité (exemple chiffré) : salaire minimal 6 240 MRU
        assertThat(tiers.admet(Montant.de(2_080), Montant.de(6_240))).isTrue();
        assertThat(tiers.admet(Montant.de(2_080), Montant.de(6_239))).isFalse();
    }

    @Test
    void s_affiche_en_pourcentage() {
        assertThat(new Fraction(1, 3).enPourcentage(2)).isEqualByComparingTo("33.33");
    }
}
