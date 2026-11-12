package com.cic.inscription.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.inscription.domain.Inscription;
import com.cic.inscription.domain.TrancheSalaire;
import com.cic.inscription.domain.TypeContrat;

class InscriptionsCsvTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");

    @Test
    void le_fichier_commence_par_le_bom_et_l_entete_puis_une_ligne_par_inscription() {
        Inscription i = new Inscription(UUID.randomUUID(), "Mariem Sidi", "22123456", "SNIM", TypeContrat.CDD,
                TrancheSalaire.DE_20000_A_40000, "Réfrigérateur", Instant.parse("2026-11-12T10:05:00Z"),
                Instant.parse("2026-11-12T10:05:00Z"));

        byte[] csv = InscriptionsCsv.ecrire(List.of(i), NOUAKCHOTT);

        assertThat(csv).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        String texte = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertThat(texte).isEqualTo(InscriptionsCsv.ENTETE + "\r\n"
                + "2026-11-12 10:05;Mariem Sidi;22123456;SNIM;CDD;DE_20000_A_40000;Réfrigérateur;2026-11-12 10:05\r\n");
    }

    @Test
    void les_separateurs_et_guillemets_sont_proteges() {
        assertThat(InscriptionsCsv.cellule("Banque; Assurance")).isEqualTo("\"Banque; Assurance\"");
        assertThat(InscriptionsCsv.cellule("Télé \"4K\"")).isEqualTo("\"Télé \"\"4K\"\"\"");
    }

    @Test
    void une_formule_saisie_sur_la_page_publique_n_est_pas_executee_par_le_tableur() {
        assertThat(InscriptionsCsv.cellule("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(InscriptionsCsv.cellule("+222")).isEqualTo("'+222");
        assertThat(InscriptionsCsv.cellule("@SOMME")).isEqualTo("'@SOMME");
        assertThat(InscriptionsCsv.cellule("SNIM")).isEqualTo("SNIM");
    }
}
