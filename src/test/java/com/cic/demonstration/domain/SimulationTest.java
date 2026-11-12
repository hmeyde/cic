package com.cic.demonstration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SimulationTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant MAINTENANT = Instant.parse("2026-11-12T14:00:00Z");
    private static final LocalDate J = LocalDate.of(2026, 11, 12);

    /** Horloge réglable en mémoire, qui garde la trace de l'instant fixé. */
    static final class HorlogeEnMemoire implements HorlogeSimulee {
        Instant fixe;
        boolean reprise;

        @Override
        public Instant maintenantReel() {
            return MAINTENANT;
        }

        @Override
        public ZoneId zone() {
            return NOUAKCHOTT;
        }

        @Override
        public void fixer(Instant instant) {
            fixe = instant;
            reprise = false;
        }

        @Override
        public void reprendre() {
            fixe = null;
            reprise = true;
        }
    }

    private final HorlogeEnMemoire horloge = new HorlogeEnMemoire();
    private final Simulation simulation = new Simulation(horloge);
    private final List<String> journal = new ArrayList<>();

    @Test
    void rejoue_les_etapes_dans_l_ordre_chronologique_avec_l_horloge_fixee_a_leur_instant() {
        simulation.planifier(J.minusDays(1), LocalTime.of(10, 0), () -> journal.add("b " + horloge.fixe));
        simulation.planifier(J.minusDays(2), LocalTime.of(10, 0), () -> journal.add("a " + horloge.fixe));
        simulation.planifier(J.minusDays(1), LocalTime.of(10, 0), () -> journal.add("c " + horloge.fixe));

        simulation.derouler();

        assertThat(journal).containsExactly("a 2026-11-10T10:00:00Z", "b 2026-11-11T10:00:00Z",
                "c 2026-11-11T10:00:00Z");
        assertThat(horloge.reprise).isTrue();
    }

    @Test
    void une_etape_peut_en_planifier_une_autre_plus_tard() {
        simulation.planifier(J.minusDays(5), LocalTime.NOON, () -> {
            journal.add("contrat");
            simulation.planifier(J.minusDays(1), LocalTime.NOON, () -> journal.add("echeance"));
        });
        simulation.planifier(J.minusDays(3), LocalTime.NOON, () -> journal.add("entre-temps"));

        simulation.derouler();

        assertThat(journal).containsExactly("contrat", "entre-temps", "echeance");
    }

    @Test
    void ignore_ce_qui_tombe_apres_l_heure_reelle() {
        assertThat(simulation.planifier(J, LocalTime.of(13, 0), () -> journal.add("ce midi"))).isTrue();
        assertThat(simulation.planifier(J, LocalTime.of(15, 0), () -> journal.add("cet après-midi"))).isFalse();

        simulation.derouler();

        assertThat(journal).containsExactly("ce midi");
        assertThat(simulation.aujourdhui()).isEqualTo(J);
    }

    @Test
    void refuse_une_etape_planifiee_avant_l_etape_en_cours_et_rend_l_heure_reelle() {
        simulation.planifier(J.minusDays(1), LocalTime.NOON,
                () -> simulation.planifier(J.minusDays(2), LocalTime.NOON, () -> journal.add("passé")));

        assertThatThrownBy(simulation::derouler).isInstanceOf(IllegalStateException.class);
        assertThat(horloge.reprise).isTrue();
        assertThat(journal).isEmpty();
    }
}
