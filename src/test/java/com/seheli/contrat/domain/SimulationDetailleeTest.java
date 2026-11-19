package com.seheli.contrat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Fraction;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.SimulationDetaillee.EcheanceSimulee;

class SimulationDetailleeTest {

    private static final DureeAutorisee DUREE = new DureeAutorisee(3, 6);
    private static final Fraction TIERS = new Fraction(1, 3);
    private static final LocalDate DEPART = LocalDate.of(2026, 11, 19);
    private static final ConditionsPrix CONDITIONS =
            new ConditionsPrix(new BigDecimal("8"), new BigDecimal("4"), new BigDecimal("40"));

    private final Prix exemple = Prix.calculer(Montant.de(20_000), CONDITIONS);

    private static Optional<SituationPaie> paie(long salaire, int jour, LocalDate fin) {
        return Optional.of(new SituationPaie(Montant.de(salaire), jour, Optional.ofNullable(fin)));
    }

    @Test
    void decompose_le_total_paye_en_prix_marge_et_zero_frais() {
        SimulationDetaillee s = SimulationDetaillee.simuler(exemple, 6, Montant.de(8_320), DUREE, DEPART,
                Optional.empty(), TIERS);

        assertThat(s.prixPublic()).isEqualTo(Montant.de(20_000));
        assertThat(s.margeSeheli()).isEqualTo(Montant.de(800));
        assertThat(s.frais()).isEqualTo(Montant.ZERO);
        assertThat(s.prixTotal()).isEqualTo(Montant.de(20_800));
        assertThat(s.resteAFinancer()).isEqualTo(Montant.de(12_480));
        assertThat(s.echeances()).extracting(EcheanceSimulee::montant).containsOnly(Montant.de(2_080));
        assertThat(s.apportSupplementaire()).isFalse();
    }

    @Test
    void date_les_echeances_au_jour_de_paie_en_ramenant_au_dernier_jour_des_mois_courts() {
        SimulationDetaillee s = SimulationDetaillee.simuler(exemple, 4, Montant.de(8_320), DUREE, DEPART,
                paie(40_000, 31, null), TIERS);

        assertThat(s.datesConnues()).isTrue();
        assertThat(s.echeances()).extracting(EcheanceSimulee::date).containsExactly(LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28));
    }

    @Test
    void un_apport_plus_eleve_reduit_les_mensualites_la_derniere_absorbant_l_ecart() {
        SimulationDetaillee s = SimulationDetaillee.simuler(exemple, 6, Montant.de(10_400), DUREE, DEPART,
                Optional.empty(), TIERS);

        assertThat(s.mensualite()).isEqualTo(Montant.de(1_733));
        assertThat(s.derniereMensualite()).isEqualTo(Montant.de(1_735));
        assertThat(s.derniereDiffere()).isTrue();
        assertThat(s.apportSupplementaire()).isTrue();
    }

    @Test
    void signale_une_mensualite_au_dela_du_tiers_du_salaire() {
        SimulationDetaillee depasse = SimulationDetaillee.simuler(exemple, 3, Montant.de(8_320), DUREE, DEPART,
                paie(12_000, 25, null), TIERS);
        SimulationDetaillee limite = SimulationDetaillee.simuler(exemple, 3, Montant.de(8_320), DUREE, DEPART,
                paie(12_480, 25, null), TIERS);

        assertThat(depasse.endettementDepasse()).isTrue();
        assertThat(depasse.plafondMensualite()).isEqualTo(Montant.de(4_000));
        assertThat(limite.endettementDepasse()).isFalse();
    }

    @Test
    void signale_une_derniere_echeance_apres_la_fin_du_cdd() {
        SimulationDetaillee s = SimulationDetaillee.simuler(exemple, 6, Montant.de(8_320), DUREE, DEPART,
                paie(40_000, 25, LocalDate.of(2027, 4, 25)), TIERS);

        assertThat(s.finContratDepassee()).isTrue();
        assertThat(SimulationDetaillee.simuler(exemple, 5, Montant.de(8_320), DUREE, DEPART,
                paie(40_000, 25, LocalDate.of(2027, 4, 25)), TIERS).finContratDepassee()).isFalse();
    }

    @Test
    void refuse_une_duree_ou_un_apport_hors_bornes() {
        assertThatThrownBy(() -> SimulationDetaillee.simuler(exemple, 7, Montant.de(8_320), DUREE, DEPART,
                Optional.empty(), TIERS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SimulationDetaillee.simuler(exemple, 6, Montant.de(8_000), DUREE, DEPART,
                Optional.empty(), TIERS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SimulationDetaillee.simuler(exemple, 6, Montant.de(20_800), DUREE, DEPART,
                Optional.empty(), TIERS)).isInstanceOf(IllegalArgumentException.class);
    }
}
