package com.seheli.contrat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.Simulation;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class SimulationServiceTest {

    private final SimulationService service = service();

    private static SimulationService service() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        return new SimulationService(new CalculPrixService(parametres), new EcheancierService(parametres));
    }

    @Test
    void simule_l_exemple_chiffre_avec_les_parametres_de_depart() {
        Simulation s = service.simuler(Montant.de(20_000), new BigDecimal("8"), 6);

        assertThat(s.prixTotal()).isEqualTo(Montant.de(20_800));
        assertThat(s.apport()).isEqualTo(Montant.de(8_320));
        assertThat(s.mensualite()).isEqualTo(Montant.de(2_080));
    }

    @Test
    void applique_les_bornes_de_duree_en_vigueur() {
        assertThatThrownBy(() -> service.simuler(Montant.de(20_000), new BigDecimal("8"), 12))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
