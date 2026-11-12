package com.cic.contrat.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.Acheteur;
import com.cic.contrat.domain.ContenuContrat;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Echeancier;
import com.cic.contrat.domain.Prix;

class OpenPdfRenduContratTest {

    private final OpenPdfRenduContrat rendu = new OpenPdfRenduContrat();

    /** Exemple chiffré de CLAUDE.md : vente 20 800, apport 8 320, 6 × 2 080. */
    private static Contrat exemple() {
        Echeancier plan = Echeancier.planifier(Montant.de(12_480), 6, LocalDate.of(2026, 11, 19), 25,
                Optional.empty());
        return Contrat.etablir("CIC-2026-000001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), plan, Instant.parse("2026-11-12T10:00:00Z"));
    }

    private static ContenuContrat contenu(Contrat contrat) {
        return new ContenuContrat(contrat,
                new Acheteur("Ould Ahmed", "Mohamed", "1234567890", LocalDate.of(1990, 5, 1), "22123456"),
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 19));
    }

    private static String texte(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extracteur = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extracteur.getTextFromPage(page)).append('\n');
            }
            return sb.toString().replace(' ', ' ').replaceAll("[ \\t]+", " ");
        } finally {
            reader.close();
        }
    }

    @Test
    void produit_un_pdf() {
        byte[] pdf = rendu.rendre(contenu(exemple()));

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void le_contrat_porte_les_parties_et_les_montants_fixes() throws IOException {
        String texte = texte(rendu.rendre(contenu(exemple())));

        assertThat(texte).contains("CONTRAT DE VENTE À TEMPÉRAMENT", "Contrat n° CIC-2026-000001 du 12/11/2026",
                "Mohamed Ould Ahmed", "NNI 1234567890", "01/05/1990", "Galaxy A35",
                "Prix de vente total", "20 800 MRU",
                "Apport payé à la commande", "8 320 MRU",
                "Reste à payer en 6 mensualités", "12 480 MRU",
                "Au plus tard le 19/11/2026");
        for (String date : List.of("25/11/2026", "25/12/2026", "25/01/2027", "25/02/2027", "25/03/2027",
                "25/04/2027")) {
            assertThat(texte).contains(date);
        }
        assertThat(texte.split("2 080 MRU", -1)).hasSize(7);
        assertThat(texte).contains("Total des mensualités", "sans intérêt ni pénalité");
    }

    @Test
    void le_prix_d_achat_et_le_capital_engage_ne_figurent_pas_sur_le_contrat() throws IOException {
        String texte = texte(rendu.rendre(contenu(exemple())));

        assertThat(texte).doesNotContain("18 400", "10 080", "2 400", "20 000");
    }

    @Test
    void ecrit_les_montants_en_ouguiyas_avec_separateur_de_milliers() {
        assertThat(OpenPdfRenduContrat.montant(Montant.de(0))).isEqualTo("0 MRU");
        assertThat(OpenPdfRenduContrat.montant(Montant.de(999))).isEqualTo("999 MRU");
        assertThat(OpenPdfRenduContrat.montant(Montant.de(2_082))).isEqualTo("2 082 MRU");
        assertThat(OpenPdfRenduContrat.montant(Montant.de(1_234_567))).isEqualTo("1 234 567 MRU");
    }
}
