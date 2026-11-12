package com.cic.inscription.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import com.cic.inscription.application.InscriptionService;
import com.cic.inscription.application.InscriptionsEnMemoire;
import com.cic.inscription.application.SaisieInscription;
import com.cic.inscription.domain.TelephoneDejaInscritException;

class InscriptionControllerTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Africa/Nouakchott"));
    private final InscriptionsEnMemoire depot = new InscriptionsEnMemoire();
    private final InscriptionController controller = new InscriptionController(new InscriptionService(depot, clock));

    private String envoyer(String telephone, String piege, ExtendedModelMap model) {
        return controller.inscrire("Mariem Sidi", telephone, "SNIM", "CDD", "DE_20000_A_40000", "Réfrigérateur",
                "on", piege, model);
    }

    @Test
    void une_inscription_est_enregistree_puis_un_doublon_est_refuse_avec_les_valeurs_gardees() {
        assertThat(envoyer("22123456", "", new ExtendedModelMap())).isEqualTo("redirect:/inscription/merci");
        assertThat(depot.toutes()).hasSize(1);

        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(envoyer("22 12 34 56", null, model)).isEqualTo("inscription/formulaire");

        assertThat(depot.toutes()).hasSize(1);
        assertThat(model.get("erreur")).isEqualTo(TelephoneDejaInscritException.CODE);
        assertThat(model.get("erreurChamp")).isEqualTo("telephone");
        assertThat(model.get("saisie")).isInstanceOfSatisfying(SaisieInscription.class,
                s -> assertThat(s.produitSouhaite()).isEqualTo("Réfrigérateur"));
    }

    @Test
    void un_robot_qui_remplit_le_champ_piege_voit_le_remerciement_mais_rien_n_est_enregistre() {
        assertThat(envoyer("22123456", "http://spam.example", new ExtendedModelMap()))
                .isEqualTo("redirect:/inscription/merci");
        assertThat(depot.toutes()).isEmpty();
    }

    @Test
    void sans_consentement_le_formulaire_est_reaffiche() {
        ExtendedModelMap model = new ExtendedModelMap();
        String vue = controller.inscrire("Mariem", "22123456", "SNIM", "CDI", "PLUS_DE_40000", "Climatiseur", null,
                null, model);

        assertThat(vue).isEqualTo("inscription/formulaire");
        assertThat(model.get("erreurChamp")).isEqualTo("consentement");
        assertThat(depot.toutes()).isEmpty();
    }
}
