package com.cic.inscription.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;

import com.cic.demande.application.DemandesEnMemoire;
import com.cic.demande.application.StatistiquesDemandeService;
import com.cic.inscription.application.InscriptionService;
import com.cic.inscription.application.InscriptionsEnMemoire;
import com.cic.inscription.application.SaisieInscription;

class InscriptionsBackOfficeControllerTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Africa/Nouakchott"));
    private final InscriptionService service = new InscriptionService(new InscriptionsEnMemoire(), clock);
    private final InscriptionsBackOfficeController controller = new InscriptionsBackOfficeController(service,
            new StatistiquesDemandeService(new DemandesEnMemoire()), clock);

    private void inscrire(String nom, String telephone) {
        service.inscrire(new SaisieInscription(nom, telephone, "SNIM", "CDI", "PLUS_DE_40000", "Téléphone", true));
    }

    @Test
    void le_tableau_affiche_le_nombre_d_inscrits_et_de_demandes_completes() {
        inscrire("Mariem", "22123456");
        inscrire("Ahmed", "33123456");
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.tableau(model)).isEqualTo("bo/inscriptions/tableau");
        assertThat(model.get("nombreInscrits")).isEqualTo(2L);
        assertThat(model.get("nombreDemandesCompletes")).isEqualTo(0L);
    }

    @Test
    void l_export_se_telecharge_en_csv_sans_mise_en_cache() {
        inscrire("Mariem", "22123456");

        ResponseEntity<byte[]> reponse = controller.export();

        HttpHeaders entetes = reponse.getHeaders();
        assertThat(entetes.getContentType()).hasToString("text/csv;charset=UTF-8");
        assertThat(entetes.getContentDisposition().isAttachment()).isTrue();
        assertThat(entetes.getContentDisposition().getFilename()).isEqualTo("inscriptions-2026-11-12.csv");
        assertThat(entetes.getCacheControl()).contains("no-store");
        assertThat(new String(reponse.getBody(), StandardCharsets.UTF_8)).contains("Mariem;22123456;SNIM;CDI");
    }
}
