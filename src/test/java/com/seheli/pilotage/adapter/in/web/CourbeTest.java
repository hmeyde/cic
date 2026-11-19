package com.seheli.pilotage.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Géométrie des courbes du tableau de bord : axe rond, ligne interrompue, miroir en arabe. */
class CourbeTest {

    private static final List<LocalDate> MOIS = List.of(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 11, 1));

    @Test
    void le_maximum_de_l_axe_est_un_multiple_rond_divisible_en_quatre() {
        assertThat(Courbe.arrondiHaut(new BigDecimal("12480"))).isEqualByComparingTo("20000");
        assertThat(Courbe.arrondiHaut(new BigDecimal("60000"))).isEqualByComparingTo("80000");
        assertThat(Courbe.arrondiHaut(new BigDecimal("8000"))).isEqualByComparingTo("8000");
        assertThat(Courbe.arrondiHaut(new BigDecimal("8001"))).isEqualByComparingTo("10000");
        assertThat(Courbe.arrondiHaut(BigDecimal.ZERO)).isEqualByComparingTo("4");
    }

    @Test
    void la_courbe_va_de_gauche_a_droite_et_les_graduations_vont_de_zero_au_maximum() {
        Courbe c = Courbe.de(MOIS, List.of(new BigDecimal("0"), new BigDecimal("10000"), new BigDecimal("20000")),
                null, null, false);

        assertThat(c.points()).extracting(Courbe.Point::x).containsExactly(72.0, 342.0, 612.0);
        assertThat(c.points().getFirst().y()).isEqualTo(c.yBase());
        assertThat(c.points().getLast().y()).isEqualTo(Courbe.MARGE_HAUT);
        assertThat(c.graduations()).extracting(g -> g.valeur().toPlainString())
                .containsExactly("0", "5000", "10000", "15000", "20000");
        assertThat(c.trace()).isEqualTo("M72.0 220.0 L342.0 124.0 L612.0 28.0");
        assertThat(c.aire()).endsWith("L612.0 220.0 L72.0 220.0 Z");
        assertThat(c.dernier()).isSameAs(c.points().getLast());
        assertThat(c.seuil()).isNull();
    }

    @Test
    void en_arabe_le_temps_va_de_droite_a_gauche() {
        Courbe c = Courbe.de(MOIS, List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE), null, null, true);

        assertThat(c.points()).extracting(Courbe.Point::x).containsExactly(568.0, 298.0, 28.0);
        assertThat(c.xAxe()).isEqualTo(568.0);
    }

    @Test
    void un_mois_sans_valeur_interrompt_la_ligne_et_supprime_le_lavis() {
        List<BigDecimal> valeurs = new ArrayList<>();
        valeurs.add(new BigDecimal("80"));
        valeurs.add(null);
        valeurs.add(new BigDecimal("100"));

        Courbe c = Courbe.de(MOIS, valeurs, new BigDecimal("90"), BigDecimal.valueOf(100), false);

        assertThat(c.trace()).isEqualTo("M72.0 66.4 M612.0 28.0");
        assertThat(c.aire()).isEmpty();
        assertThat(c.points().get(1).aValeur()).isFalse();
        assertThat(c.seuil().y()).isEqualTo(47.2);
        assertThat(c.graduations().getLast().valeur()).isEqualByComparingTo("100");
    }
}
