package com.cic.contrat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.Prix;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;
import com.cic.parametrage.domain.Parametre;

class CalculPrixServiceTest {

    private static CalculPrixService service(List<Parametre> lignes) {
        ParametreService parametres = new ParametreService(() -> lignes);
        parametres.afterSingletonsInstantiated();
        return new CalculPrixService(parametres);
    }

    private final CalculPrixService service = service(ParametresDeDepart.lignes());

    @Test
    void calcule_l_exemple_chiffre_avec_les_parametres_de_depart() {
        Prix prix = service.calculer(Montant.de(20_000), new BigDecimal("8"));

        assertThat(prix.prixAchat()).isEqualTo(Montant.de(18_400));
        assertThat(prix.prixVente()).isEqualTo(Montant.de(20_800));
        assertThat(prix.apportMinimum()).isEqualTo(Montant.de(8_320));
    }

    @Test
    void applique_la_marge_de_la_categorie() {
        Prix prix = service.calculer(Montant.de(20_000), new BigDecimal("8"), new BigDecimal("5"));

        assertThat(prix.prixVente()).isEqualTo(Montant.de(21_000));
    }

    @Test
    void lit_l_apport_minimum_dans_les_parametres() {
        CalculPrixService a50 = service(ParametresDeDepart.avec(CleParametre.APPORT_MINIMUM_PCT, "50"));

        assertThat(a50.calculer(Montant.de(20_000), new BigDecimal("8")).apportMinimum())
                .isEqualTo(Montant.de(10_400));
    }

    @Test
    void refuse_une_remise_hors_des_bornes_parametrees() {
        assertThatThrownBy(() -> service.calculer(Montant.de(20_000), new BigDecimal("4")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.calculer(Montant.de(20_000), new BigDecimal("11")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.calculer(Montant.de(20_000), new BigDecimal("10")).prixAchat())
                .isEqualTo(Montant.de(18_000));
    }
}
