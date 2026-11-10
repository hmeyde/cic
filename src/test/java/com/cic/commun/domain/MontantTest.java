package com.cic.commun.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MontantTest {

    @Nested
    class Creation {

        @ParameterizedTest
        @CsvSource({
                "1250.4, 1250",
                "1250.5, 1251",
                "1250.49, 1250",
                "0.5, 1",
                "0.4, 0",
                "20800, 20800"
        })
        void arrondit_a_l_ouguiya_half_up(String entree, long attendu) {
            assertThat(Montant.de(entree)).isEqualTo(Montant.de(attendu));
            assertThat(Montant.de(new BigDecimal(entree)).valeur()).isEqualByComparingTo(BigDecimal.valueOf(attendu));
        }

        @Test
        void la_valeur_est_toujours_a_l_echelle_zero() {
            assertThat(Montant.de(new BigDecimal("100.00")).valeur().scale()).isZero();
        }

        @Test
        void l_egalite_ne_depend_pas_de_l_echelle_d_entree() {
            Montant a = Montant.de(new BigDecimal("100.00"));
            Montant b = Montant.de(100);
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }

        @Test
        void refuse_un_montant_negatif() {
            assertThatThrownBy(() -> Montant.de(-1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Montant.de("-0.6")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void un_negatif_arrondi_a_zero_est_accepte() {
            assertThat(Montant.de("-0.4")).isEqualTo(Montant.ZERO);
        }

        @Test
        void refuse_null() {
            assertThatThrownBy(() -> Montant.de((BigDecimal) null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Montant.de((String) null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void refuse_un_texte_qui_n_est_pas_un_nombre() {
            assertThatThrownBy(() -> Montant.de("abc")).isInstanceOf(NumberFormatException.class);
        }

        @Test
        void s_affiche_en_mru() {
            assertThat(Montant.de(20800)).hasToString("20800 MRU");
            assertThat(Montant.ZERO).hasToString("0 MRU");
        }
    }

    @Nested
    class Operations {

        @Test
        void additionne() {
            assertThat(Montant.de(18400).plus(Montant.de(2400))).isEqualTo(Montant.de(20800));
        }

        @Test
        void soustrait() {
            assertThat(Montant.de(20800).moins(Montant.de(8320))).isEqualTo(Montant.de(12480));
        }

        @Test
        void refuse_une_soustraction_negative() {
            assertThatThrownBy(() -> Montant.de(100).moins(Montant.de(101)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void multiplie_et_arrondit_une_seule_fois() {
            assertThat(Montant.de(2080).fois(6)).isEqualTo(Montant.de(12480));
            // 333 × 1,5 = 499,5 → 500
            assertThat(Montant.de(333).fois(new BigDecimal("1.5"))).isEqualTo(Montant.de(500));
        }

        @Test
        void calcule_un_pourcentage() {
            assertThat(Montant.de(20000).pourcentage(new BigDecimal("8"))).isEqualTo(Montant.de(1600));
            assertThat(Montant.de(20800).pourcentage(new BigDecimal("40"))).isEqualTo(Montant.de(8320));
            // 1 013 × 4 % = 40,52 → 41
            assertThat(Montant.de(1013).pourcentage(new BigDecimal("4"))).isEqualTo(Montant.de(41));
            // 1 012 × 2,5 % = 25,30 → 25 ; taux décimal accepté
            assertThat(Montant.de(1012).pourcentage(new BigDecimal("2.5"))).isEqualTo(Montant.de(25));
        }

        @Test
        void est_immuable() {
            Montant prix = Montant.de(20000);
            prix.plus(Montant.de(800));
            assertThat(prix).isEqualTo(Montant.de(20000));
        }
    }

    @Nested
    class Repartition {

        @Test
        void repartit_exactement_quand_la_division_tombe_juste() {
            assertThat(Montant.de(12480).repartir(6)).containsExactly(
                    Montant.de(2080), Montant.de(2080), Montant.de(2080),
                    Montant.de(2080), Montant.de(2080), Montant.de(2080));
        }

        @Test
        void la_derniere_part_absorbe_l_ecart_par_defaut() {
            // 1 000 / 3 = 333,33 → 333 ; dernière = 334
            assertThat(Montant.de(1000).repartir(3))
                    .containsExactly(Montant.de(333), Montant.de(333), Montant.de(334));
        }

        @Test
        void la_derniere_part_absorbe_l_ecart_par_exces() {
            // 1 001 / 6 = 166,83 → 167 ; dernière = 1 001 − 5 × 167 = 166
            assertThat(Montant.de(1001).repartir(6)).containsExactly(
                    Montant.de(167), Montant.de(167), Montant.de(167),
                    Montant.de(167), Montant.de(167), Montant.de(166));
        }

        @ParameterizedTest
        @CsvSource({"12481, 3", "12481, 4", "12481, 5", "12485, 6", "7, 6", "10000, 1"})
        void la_somme_des_parts_vaut_le_montant(long total, int nombre) {
            List<Montant> parts = Montant.de(total).repartir(nombre);
            assertThat(parts).hasSize(nombre);
            assertThat(parts.stream().reduce(Montant.ZERO, Montant::plus)).isEqualTo(Montant.de(total));
        }

        @Test
        void la_liste_est_non_modifiable() {
            List<Montant> parts = Montant.de(100).repartir(2);
            assertThatThrownBy(() -> parts.add(Montant.ZERO)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void refuse_un_nombre_de_parts_nul_ou_negatif() {
            assertThatThrownBy(() -> Montant.de(100).repartir(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Montant.de(100).repartir(-1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void refuse_un_montant_trop_petit_pour_la_repartition() {
            // 9 / 6 = 1,5 → 2 ; dernière = 9 − 5 × 2 = −1
            assertThatThrownBy(() -> Montant.de(9).repartir(6)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Comparaison {

        @Test
        void compare_deux_montants() {
            Montant petit = Montant.de(100);
            Montant grand = Montant.de(200);

            assertThat(grand.estSuperieurA(petit)).isTrue();
            assertThat(petit.estInferieurA(grand)).isTrue();
            assertThat(petit.estSuperieurA(grand)).isFalse();
            assertThat(petit.estInferieurOuEgalA(Montant.de(100))).isTrue();
            assertThat(petit.estSuperieurOuEgalA(Montant.de(100))).isTrue();
            assertThat(petit).isLessThan(grand);
        }

        @Test
        void reconnait_zero() {
            assertThat(Montant.ZERO.estZero()).isTrue();
            assertThat(Montant.de(1).estZero()).isFalse();
        }
    }

    /** Exemple chiffré de CLAUDE.md. */
    @Test
    void exemple_chiffre_de_reference() {
        Montant prixPublic = Montant.de(20000);
        Montant prixAchat = prixPublic.moins(prixPublic.pourcentage(new BigDecimal("8")));
        Montant prixVente = prixPublic.plus(prixPublic.pourcentage(new BigDecimal("4")));
        Montant apport = prixVente.pourcentage(new BigDecimal("40"));
        Montant reste = prixVente.moins(apport);
        List<Montant> mensualites = reste.repartir(6);
        Montant capitalEngage = prixAchat.moins(apport);
        Montant beneficeBrut = prixVente.moins(prixAchat);

        assertThat(prixAchat).isEqualTo(Montant.de(18400));
        assertThat(prixVente).isEqualTo(Montant.de(20800));
        assertThat(apport).isEqualTo(Montant.de(8320));
        assertThat(reste).isEqualTo(Montant.de(12480));
        assertThat(mensualites).hasSize(6).containsOnly(Montant.de(2080));
        assertThat(capitalEngage).isEqualTo(Montant.de(10080));
        assertThat(beneficeBrut).isEqualTo(Montant.de(2400));
    }
}
