package com.cic.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Vérifie les critères d'installabilité de la PWA : manifest complet et icônes PNG 192 et 512. */
class ManifestPwaTest {

    private static JsonNode manifest() throws Exception {
        try (InputStream in = ressource("/static/manifest.webmanifest")) {
            return JsonMapper.builder().build().readTree(in);
        }
    }

    private static InputStream ressource(String chemin) {
        InputStream in = ManifestPwaTest.class.getResourceAsStream(chemin);
        assertThat(in).as(chemin).isNotNull();
        return in;
    }

    @Test
    void le_manifest_contient_les_champs_requis_pour_l_installation() throws Exception {
        JsonNode m = manifest();
        assertThat(m.path("name").asString()).isNotBlank();
        assertThat(m.path("short_name").asString()).isEqualTo("CIC");
        assertThat(m.path("start_url").asString()).startsWith("/");
        assertThat(m.path("display").asString()).isIn("standalone", "fullscreen", "minimal-ui");
    }

    @Test
    void chaque_icone_png_existe_et_a_la_taille_declaree() throws Exception {
        List<String> tailles = new ArrayList<>();
        for (JsonNode icone : manifest().path("icons")) {
            String src = icone.path("src").asString();
            try (InputStream in = ressource("/static" + src)) {
                if (!"image/png".equals(icone.path("type").asString())) {
                    continue;
                }
                BufferedImage image = ImageIO.read(in);
                String taille = image.getWidth() + "x" + image.getHeight();
                assertThat(icone.path("sizes").asString()).as(src).isEqualTo(taille);
                tailles.add(taille + " " + icone.path("purpose").asString());
            }
        }
        assertThat(tailles).contains("192x192 any", "512x512 any", "512x512 maskable");
    }

    @Test
    void le_service_worker_et_la_page_hors_ligne_sont_publies() throws Exception {
        try (InputStream sw = ressource("/static/sw.js"); InputStream page = ressource("/static/hors-ligne.html")) {
            assertThat(new String(sw.readAllBytes())).contains("addEventListener(\"fetch\"");
        }
    }
}
