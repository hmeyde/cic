package com.cic.contrat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;

class SimulationTest {

    private static final DureeAutorisee DUREE = new DureeAutorisee(3, 6);
    private static final ConditionsPrix CONDITIONS =
            new ConditionsPrix(new BigDecimal("8"), new BigDecimal("4"), new BigDecimal("40"));

    private final Prix exemple = Prix.calculer(Montant.de(20_000), CONDITIONS);

    @Test
    void simule_l_exemple_chiffre_sur_6_mois() {
        Simulation s = Simulation.simuler(exemple, 6, DUREE);

        assertThat(s.nombreMois()).isEqualTo(6);
        assertThat(s.prixTotal()).isEqualTo(Montant.de(20_800));
        assertThat(s.apport()).isEqualTo(Montant.de(8_320));
        assertThat(s.resteAFinancer()).isEqualTo(Montant.de(12_480));
        assertThat(s.mensualite()).isEqualTo(Montant.de(2_080));
        assertThat(s.derniereDiffere()).isFalse();
    }

    @Test
    void le_prix_total_ne_depend_pas_de_la_duree() {
        for (int mois = 3; mois <= 6; mois++) {
            assertThat(Simulation.simuler(exemple, mois, DUREE).prixTotal()).isEqualTo(Montant.de(20_800));
        }
        assertThat(Simulation.simuler(exemple, 3, DUREE).mensualite()).isEqualTo(Montant.de(4_160));
    }

    @Test
    void la_derniere_mensualite_absorbe_l_ecart_d_arrondi() {
        // Reste non divisible : 20 801 − 8 320 = 12 481 sur 3 mois → 4 160, 4 160, 4 161.
        Prix prix = new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_801), Montant.de(8_320));

        Simulation s = Simulation.simuler(prix, 3, DUREE);

        assertThat(s.mensualite()).isEqualTo(Montant.de(4_160));
        assertThat(s.derniereMensualite()).isEqualTo(Montant.de(4_161));
        assertThat(s.derniereDiffere()).isTrue();
        assertThat(s.apport().plus(s.mensualite()).plus(s.mensualite()).plus(s.derniereMensualite()))
                .isEqualTo(s.prixTotal());
    }

    @Test
    void refuse_une_duree_hors_bornes() {
        assertThatThrownBy(() -> Simulation.simuler(exemple, 2, DUREE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Simulation.simuler(exemple, 7, DUREE)).isInstanceOf(IllegalArgumentException.class);
    }
}
