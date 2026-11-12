package com.cic.recouvrement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CalendrierRelancesTest {

    private static final LocalDate ECHEANCE = LocalDate.of(2026, 11, 1);
    private final CalendrierRelances calendrier = new CalendrierRelances(3, 7);

    @Test
    void premiere_relance_a_j_plus_3_rattrapee_jusqu_a_la_veille_de_la_seconde() {
        assertThat(calendrier.relanceDue(ECHEANCE, ECHEANCE.plusDays(2))).isEmpty();
        assertThat(calendrier.relanceDue(ECHEANCE, ECHEANCE.plusDays(3))).contains(TypeRelance.ECHEC_PREMIERE);
        assertThat(calendrier.relanceDue(ECHEANCE, ECHEANCE.plusDays(6))).contains(TypeRelance.ECHEC_PREMIERE);
    }

    @Test
    void seconde_relance_a_j_plus_7_rattrapee_une_semaine() {
        assertThat(calendrier.relanceDue(ECHEANCE, ECHEANCE.plusDays(7))).contains(TypeRelance.ECHEC_SECONDE);
        assertThat(calendrier.relanceDue(ECHEANCE, ECHEANCE.plusDays(13))).contains(TypeRelance.ECHEC_SECONDE);
        assertThat(calendrier.relanceDue(ECHEANCE, ECHEANCE.plusDays(14))).isEmpty();
    }

    @Test
    void fenetre_des_echeances_a_relancer() {
        LocalDate aujourdhui = LocalDate.of(2026, 11, 20);
        assertThat(calendrier.plusRecenteEcheance(aujourdhui)).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(calendrier.plusAncienneEcheance(aujourdhui)).isEqualTo(LocalDate.of(2026, 11, 7));
        assertThat(calendrier.relanceDue(LocalDate.of(2026, 11, 7), aujourdhui)).contains(TypeRelance.ECHEC_SECONDE);
    }

    @Test
    void la_premiere_relance_precede_la_seconde() {
        assertThatThrownBy(() -> new CalendrierRelances(7, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CalendrierRelances(0, 7)).isInstanceOf(IllegalArgumentException.class);
    }
}
