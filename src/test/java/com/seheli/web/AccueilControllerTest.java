package com.seheli.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.CalculPrixService;
import com.seheli.contrat.application.EcheancierService;
import com.seheli.contrat.application.SimulationService;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class AccueilControllerTest {

    private static AccueilController controleur() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        SimulationService simulation =
                new SimulationService(new CalculPrixService(parametres), new EcheancierService(parametres));
        return new AccueilController(simulation, parametres);
    }

    @Test
    void l_exemple_chiffre_vient_du_calcul_du_domaine() {
        ExempleAccueil exemple = controleur().exemple();

        assertThat(exemple.prixTotal()).isEqualTo(Montant.de(20_800));
        assertThat(exemple.apport()).isEqualTo(Montant.de(8_320));
        assertThat(exemple.nombreMois()).isEqualTo(6);
        assertThat(exemple.mensualite()).isEqualTo(Montant.de(2_080));
        assertThat(exemple.derniereDiffere()).isFalse();
    }

    @Test
    void les_points_cles_viennent_des_parametres() {
        ExempleAccueil exemple = controleur().exemple();

        assertThat(exemple.apportPctTexte()).isEqualTo("40");
        assertThat(exemple.dureeMinMois()).isEqualTo(3);
        assertThat(exemple.dureeMaxMois()).isEqualTo(6);
    }
}
