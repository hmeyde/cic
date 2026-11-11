package com.cic.contrat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;

class PrixTest {

    private static final ConditionsPrix EXEMPLE =
            new ConditionsPrix(new BigDecimal("8"), new BigDecimal("4"), new BigDecimal("40"));

    private final Prix prix = Prix.calculer(Montant.de(20_000), EXEMPLE);

    @Test
    void calcule_l_exemple_chiffre_de_reference() {
        assertThat(prix.prixPublic()).isEqualTo(Montant.de(20_000));
        assertThat(prix.prixAchat()).isEqualTo(Montant.de(18_400));
        assertThat(prix.prixVente()).isEqualTo(Montant.de(20_800));
        assertThat(prix.apportMinimum()).isEqualTo(Montant.de(8_320));
        assertThat(prix.resteAFinancer()).isEqualTo(Montant.de(12_480));
        assertThat(prix.resteAFinancer().repartir(6)).containsOnly(Montant.de(2_080));
        assertThat(prix.capitalEngage()).isEqualTo(Montant.de(10_080));
        assertThat(prix.beneficeBrut()).isEqualTo(Montant.de(2_400));
    }

    @Test
    void arrondit_chaque_montant_a_l_ouguiya() {
        Prix p = Prix.calculer(Montant.de(12_345), EXEMPLE);

        assertThat(p.prixAchat()).isEqualTo(Montant.de(11_357));   // 12 345 − 987,6 → 988
        assertThat(p.prixVente()).isEqualTo(Montant.de(12_839));   // 12 345 + 493,8 → 494
        assertThat(p.apportMinimum()).isEqualTo(Montant.de(5_136)); // 40 % de 12 839 = 5 135,6
    }

    @Test
    void un_apport_superieur_reduit_le_reste_et_le_capital_engage() {
        Montant apport = Montant.de(10_000);

        assertThat(prix.accepteApport(apport)).isTrue();
        assertThat(prix.resteAFinancer(apport)).isEqualTo(Montant.de(10_800));
        assertThat(prix.capitalEngage(apport)).isEqualTo(Montant.de(8_400));
    }

    @Test
    void le_capital_engage_est_nul_si_l_apport_couvre_le_prix_d_achat() {
        assertThat(prix.capitalEngage(Montant.de(19_000))).isEqualTo(Montant.ZERO);
    }

    @Test
    void refuse_un_apport_inferieur_au_minimum() {
        Montant apport = Montant.de(8_319);

        assertThat(prix.accepteApport(apport)).isFalse();
        assertThatThrownBy(() -> prix.resteAFinancer(apport)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_apport_superieur_au_prix_de_vente() {
        assertThat(prix.accepteApport(Montant.de(20_801))).isFalse();
        assertThat(prix.accepteApport(Montant.de(20_800))).isTrue();
    }

    @Test
    void refuse_un_prix_public_nul() {
        assertThatThrownBy(() -> Prix.calculer(Montant.ZERO, EXEMPLE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_des_taux_hors_bornes() {
        BigDecimal ok = new BigDecimal("4");
        assertThatThrownBy(() -> new ConditionsPrix(new BigDecimal("-1"), ok, ok))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConditionsPrix(new BigDecimal("100"), ok, ok))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConditionsPrix(ok, new BigDecimal("-0.5"), ok))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConditionsPrix(ok, ok, new BigDecimal("101")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
