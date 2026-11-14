package com.cic.demande.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import com.cic.commun.domain.Montant;
import com.cic.demande.domain.CommandeFournisseur;
import com.cic.demande.domain.ContenuBonCommande;

class OpenPdfRenduBonCommandeTest {

    @Test
    void le_bon_de_commande_porte_le_fournisseur_le_produit_et_le_net_a_payer() throws IOException {
        CommandeFournisseur c = new CommandeFournisseur(UUID.randomUUID(), "BC-2026-000001", UUID.randomUUID(),
                UUID.randomUUID(), "SEH-2026-000001", UUID.randomUUID(), UUID.randomUUID(), "Galaxy A35",
                Montant.de(18_400), "admin", Instant.parse("2026-11-15T10:00:00Z"));

        byte[] pdf = new OpenPdfRenduBonCommande().rendre(new ContenuBonCommande(c, "Mauritel Store",
                Montant.de(20_000), LocalDate.of(2026, 11, 15), "Seheli"));

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(pdf);
        String texte;
        try {
            texte = new PdfTextExtractor(reader).getTextFromPage(1).replace(' ', ' ');
        } finally {
            reader.close();
        }
        assertThat(texte).contains("BON DE COMMANDE", "BC-2026-000001", "Mauritel Store", "Galaxy A35",
                "SEH-2026-000001", "20 000 MRU", "1 600 MRU", "18 400 MRU", "15/11/2026", "Seheli", "Pour Seheli");
        assertThat(texte).doesNotContain("CIC");
    }
}
