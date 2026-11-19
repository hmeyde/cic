package com.seheli.contrat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.seheli.commun.domain.ErreurMetier;
import com.seheli.commun.domain.Fraction;
import com.seheli.commun.domain.Montant;

class EcheancierTest {

    private static final DureeAutorisee DUREE = new DureeAutorisee(3, 6);
    private static final LocalDate DEPART = LocalDate.of(2026, 11, 11);

    private static Echeancier etablir(long reste, int mois) {
        return Echeancier.etablir(Montant.de(reste), mois, DUREE, DEPART, 25, Optional.empty());
    }

    @Test
    void exemple_chiffre_six_mensualites_de_2080() {
        Echeancier e = etablir(12_480, 6);

        assertThat(e.nombre()).isEqualTo(6);
        assertThat(e.echeances()).extracting(EcheancePrevue::montant).containsOnly(Montant.de(2_080));
        assertThat(e.echeances()).extracting(EcheancePrevue::rang).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(e.total()).isEqualTo(Montant.de(12_480));
        assertThat(e.mensualite()).isEqualTo(Montant.de(2_080));
    }

    @Test
    void la_derniere_echeance_absorbe_l_ecart_vers_le_bas() {
        // 10 000 / 3 = 3 333,33 → 3 333 ; dernière = 10 000 − 6 666 = 3 334
        Echeancier e = etablir(10_000, 3);

        assertThat(e.echeances()).extracting(EcheancePrevue::montant)
                .containsExactly(Montant.de(3_333), Montant.de(3_333), Montant.de(3_334));
        assertThat(e.total()).isEqualTo(Montant.de(10_000));
    }

    @Test
    void la_derniere_echeance_absorbe_l_ecart_vers_le_haut() {
        // 10 001 / 6 = 1 666,83 → 1 667 ; dernière = 10 001 − 8 335 = 1 666
        Echeancier e = etablir(10_001, 6);

        assertThat(e.echeances().subList(0, 5)).extracting(EcheancePrevue::montant).containsOnly(Montant.de(1_667));
        assertThat(e.derniere().montant()).isEqualTo(Montant.de(1_666));
        assertThat(e.total()).isEqualTo(Montant.de(10_001));
    }

    @Test
    void arrondit_la_demi_ouguiya_au_superieur() {
        // 2 502 / 4 = 625,5 → 626 (HALF_UP) ; dernière = 2 502 − 1 878 = 624
        Echeancier e = etablir(2_502, 4);

        assertThat(e.echeances()).extracting(EcheancePrevue::montant)
                .containsExactly(Montant.de(626), Montant.de(626), Montant.de(626), Montant.de(624));
        assertThat(e.total()).isEqualTo(Montant.de(2_502));
    }

    @ParameterizedTest
    @ValueSource(longs = {12_480, 10_000, 10_001, 7, 99_999, 1_234_567})
    void la_somme_des_echeances_egale_toujours_le_reste(long reste) {
        for (int mois = 3; mois <= 6; mois++) {
            assertThat(etablir(reste, mois).total()).isEqualTo(Montant.de(reste));
        }
    }

    @Test
    void les_dates_tombent_au_jour_de_paie_a_partir_du_mois_courant() {
        Echeancier e = etablir(12_480, 3);

        assertThat(e.echeances()).extracting(EcheancePrevue::date).containsExactly(
                LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 25));
    }

    @Test
    void commence_le_mois_suivant_si_le_jour_de_paie_est_passe_ou_atteint() {
        Echeancier passe = Echeancier.etablir(Montant.de(9_000), 3, DUREE, DEPART, 5, Optional.empty());
        Echeancier memeJour = Echeancier.etablir(Montant.de(9_000), 3, DUREE, DEPART, 11, Optional.empty());

        assertThat(passe.premiere().date()).isEqualTo(LocalDate.of(2026, 12, 5));
        assertThat(memeJour.premiere().date()).isEqualTo(LocalDate.of(2026, 12, 11));
    }

    @Test
    void jour_de_paie_ramene_au_dernier_jour_des_mois_courts() {
        Echeancier e = Echeancier.etablir(Montant.de(9_000), 4, DUREE,
                LocalDate.of(2027, 12, 31), 31, Optional.empty());

        assertThat(e.echeances()).extracting(EcheancePrevue::date).containsExactly(
                LocalDate.of(2028, 1, 31), LocalDate.of(2028, 2, 29),
                LocalDate.of(2028, 3, 31), LocalDate.of(2028, 4, 30));
    }

    @Test
    void cdd_accepte_si_la_derniere_echeance_tombe_avant_la_fin_du_contrat() {
        Echeancier e = Echeancier.etablir(Montant.de(12_480), 6, DUREE, DEPART, 25,
                Optional.of(LocalDate.of(2027, 4, 26)));

        assertThat(e.derniere().date()).isEqualTo(LocalDate.of(2027, 4, 25));
    }

    @Test
    void cdd_refuse_si_la_derniere_echeance_tombe_le_jour_de_fin_du_contrat() {
        assertThatThrownBy(() -> Echeancier.etablir(Montant.de(12_480), 6, DUREE, DEPART, 25,
                Optional.of(LocalDate.of(2027, 4, 25))))
                .isInstanceOfSatisfying(FinContratDepasseeException.class, ex -> {
                    assertThat(ex).isInstanceOf(ErreurMetier.class);
                    assertThat(ex.code()).isEqualTo(FinContratDepasseeException.CODE);
                    assertThat(ex.derniereEcheance()).isEqualTo(LocalDate.of(2027, 4, 25));
                    assertThat(ex.finContrat()).isEqualTo(LocalDate.of(2027, 4, 25));
                })
                .hasMessageContaining("fin du contrat");
    }

    @Test
    void cdd_refuse_si_le_contrat_finit_avant_la_derniere_echeance() {
        assertThatThrownBy(() -> Echeancier.etablir(Montant.de(12_480), 6, DUREE, DEPART, 25,
                Optional.of(LocalDate.of(2027, 2, 1))))
                .isInstanceOf(FinContratDepasseeException.class);
    }

    @Test
    void cdd_une_duree_plus_courte_passe_avant_la_fin_du_contrat() {
        Optional<LocalDate> fin = Optional.of(LocalDate.of(2027, 3, 1));

        assertThatThrownBy(() -> Echeancier.etablir(Montant.de(12_480), 6, DUREE, DEPART, 25, fin))
                .isInstanceOf(FinContratDepasseeException.class);
        Echeancier e = Echeancier.etablir(Montant.de(12_480), 3, DUREE, DEPART, 25, fin);
        assertThat(e.derniere().date()).isEqualTo(LocalDate.of(2027, 1, 25));
    }

    // Contrôle d'endettement : mensualités Seheli ≤ 1/3 du salaire net

    private static final Fraction TIERS = new Fraction(1, 3);

    @Test
    void endettement_accepte_une_mensualite_egale_au_tiers_du_salaire() {
        // 2 080 × 3 = 6 240 : exactement un tiers
        Echeancier e = etablir(12_480, 6);

        assertThatCode(() -> e.verifierEndettement(Montant.de(6_240), Montant.ZERO, TIERS))
                .doesNotThrowAnyException();
    }

    @Test
    void endettement_refuse_une_mensualite_superieure_au_tiers_du_salaire() {
        Echeancier e = etablir(12_480, 6);

        assertThatThrownBy(() -> e.verifierEndettement(Montant.de(6_239), Montant.ZERO, TIERS))
                .isInstanceOfSatisfying(EndettementDepasseException.class, ex -> {
                    assertThat(ex).isInstanceOf(ErreurMetier.class);
                    assertThat(ex.code()).isEqualTo(EndettementDepasseException.CODE);
                    assertThat(ex.totalMensualites()).isEqualTo(Montant.de(2_080));
                    assertThat(ex.salaireNet()).isEqualTo(Montant.de(6_239));
                    assertThat(ex.partMax()).isEqualTo(TIERS);
                });
    }

    @Test
    void endettement_compte_les_mensualites_seheli_deja_en_cours() {
        Echeancier e = etablir(12_480, 6);

        // 1 000 + 2 080 = 3 080 ; 3 080 × 3 = 9 240
        assertThatCode(() -> e.verifierEndettement(Montant.de(9_240), Montant.de(1_000), TIERS))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> e.verifierEndettement(Montant.de(9_239), Montant.de(1_000), TIERS))
                .isInstanceOfSatisfying(EndettementDepasseException.class,
                        ex -> assertThat(ex.totalMensualites()).isEqualTo(Montant.de(3_080)));
    }

    @Test
    void endettement_retient_la_mensualite_la_plus_elevee() {
        // 12 482 / 6 = 2 080,33 → 2 080 ; la dernière absorbe l'écart : 2 082
        Echeancier e = etablir(12_482, 6);
        assertThat(e.mensualite()).isEqualTo(Montant.de(2_080));
        assertThat(e.mensualiteMaximale()).isEqualTo(Montant.de(2_082));

        assertThatCode(() -> e.verifierEndettement(Montant.de(6_246), Montant.ZERO, TIERS))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> e.verifierEndettement(Montant.de(6_245), Montant.ZERO, TIERS))
                .isInstanceOf(EndettementDepasseException.class);
    }

    @Test
    void endettement_compare_au_tiers_exact_et_non_a_33_pourcent() {
        // 33 % de 6 240 = 2 059,2 < 2 080 ; le tiers exact admet 2 080
        Echeancier e = etablir(12_480, 6);

        assertThatCode(() -> e.verifierEndettement(Montant.de(6_240), Montant.ZERO, TIERS))
                .doesNotThrowAnyException();
    }

    @Test
    void endettement_refuse_un_salaire_nul() {
        Echeancier e = etablir(12_480, 6);

        assertThatThrownBy(() -> e.verifierEndettement(Montant.ZERO, Montant.ZERO, TIERS))
                .isInstanceOf(EndettementDepasseException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 7, 12})
    void refuse_une_duree_hors_bornes(int mois) {
        assertThatThrownBy(() -> etablir(12_480, mois))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hors bornes");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 32})
    void refuse_un_jour_de_paie_invalide(int jour) {
        assertThatThrownBy(() -> Echeancier.etablir(Montant.de(12_480), 6, DUREE, DEPART, jour, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_reste_nul_ou_trop_petit() {
        assertThatThrownBy(() -> etablir(0, 3)).isInstanceOf(IllegalArgumentException.class);
        // 5 / 6 = 0,83 → 1 ; dernière = 5 − 5 = 0
        assertThatThrownBy(() -> etablir(5, 6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void la_liste_des_echeances_est_immuable() {
        Echeancier e = etablir(12_480, 6);

        assertThatThrownBy(() -> e.echeances().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
