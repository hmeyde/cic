package com.seheli.pilotage.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;
import com.seheli.pilotage.domain.Concentration;

/** Barres de concentration : groupe « Autres », dépassement du seuil, miroir en arabe. */
class BarresTest {

    private static Concentration c(String libelle, long montant, String part) {
        return new Concentration(Optional.ofNullable(libelle), Montant.de(montant), new BigDecimal(part));
    }

    @Test
    void au_dela_de_cinq_groupes_les_plus_petits_sont_regroupes_dans_autres() {
        List<Concentration> repartition = List.of(c("A", 4_000, "40.0"), c("B", 2_000, "20.0"),
                c("C", 1_500, "15.0"), c("D", 1_000, "10.0"), c("E", 1_000, "10.0"), c(null, 500, "5.0"));

        Barres b = Barres.de(repartition, new BigDecimal("20"), false);

        assertThat(b.lignes()).hasSize(5);
        assertThat(b.lignes()).extracting(Barres.Ligne::libelle).containsExactly("A", "B", "C", "D", null);
        Barres.Ligne autres = b.lignes().getLast();
        assertThat(autres.autres()).isTrue();
        assertThat(autres.nombreAutres()).isEqualTo(2);
        assertThat(autres.montant()).isEqualTo(Montant.de(1_500));
        assertThat(autres.partPct()).isEqualByComparingTo("15.0");
        assertThat(b.lignes()).extracting(Barres.Ligne::depasse).containsExactly(true, false, false, false, false);
        assertThat(b.echellePct()).isEqualByComparingTo("50");
        assertThat(b.xSeuil()).isEqualTo(196.0);
    }

    @Test
    void la_barre_part_de_la_gauche_en_francais_et_de_la_droite_en_arabe() {
        List<Concentration> repartition = List.of(c("A", 1_000, "100.0"));

        Barres ltr = Barres.de(repartition, new BigDecimal("20"), false);
        Barres rtl = Barres.de(repartition, new BigDecimal("20"), true);

        assertThat(ltr.echellePct()).isEqualByComparingTo("100");
        assertThat(ltr.lignes().getFirst().forme()).startsWith("M0.0 24.0 h486.0 a4.0 4.0 0 0 1 4.0 4.0");
        assertThat(rtl.lignes().getFirst().forme()).startsWith("M640.0 24.0 h-486.0 a4.0 4.0 0 0 0 -4.0 4.0");
        assertThat(rtl.xDepart()).isEqualTo(640.0);
        assertThat(rtl.lignes().getFirst().xValeur()).isEqualTo(142.0);
    }

    @Test
    void sans_encours_il_n_y_a_aucune_barre() {
        assertThat(Barres.de(List.of(), new BigDecimal("20"), false).lignes()).isEmpty();
    }
}
