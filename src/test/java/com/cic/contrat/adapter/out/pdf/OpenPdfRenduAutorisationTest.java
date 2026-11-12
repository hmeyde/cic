package com.cic.contrat.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.cic.contrat.domain.AutorisationPrelevement;
import com.cic.contrat.domain.CompteBeneficiaire;
import com.cic.contrat.domain.ContenuAutorisation;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Echeancier;
import com.cic.contrat.domain.Prix;
import com.cic.contrat.domain.StatutAutorisation;
import com.cic.contrat.domain.TypeAutorisation;

class OpenPdfRenduAutorisationTest {

    private static final Instant ETABLI_LE = Instant.parse("2026-11-12T10:00:00Z");

    private final OpenPdfRenduAutorisation rendu = new OpenPdfRenduAutorisation();

    /** Exemple chiffré de CLAUDE.md : reste 12 480 = 6 × 2 080. */
    private static Contrat exemple() {
        Echeancier plan = Echeancier.planifier(Montant.de(12_480), 6, LocalDate.of(2026, 11, 19), 25,
                Optional.empty());
        return Contrat.etablir("CIC-2026-000001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), plan, ETABLI_LE);
    }

    private static ContenuAutorisation contenu(Contrat contrat, AutorisationPrelevement a, Optional<String> banque) {
        return new ContenuAutorisation(a, contrat,
                new Acheteur("Ould Ahmed", "Mohamed", "1234567890", LocalDate.of(1990, 5, 1), "22123456"),
                new CompteBeneficiaire("CIC SARL", "BMCI", "00020 00101 00001234567 53"), banque,
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 19));
    }

    private static ContenuAutorisation contenu(Optional<String> banque) {
        Contrat contrat = exemple();
        return contenu(contrat, AutorisationPrelevement.emettre(contrat, ETABLI_LE), banque);
    }

    private static String texte(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extracteur = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extracteur.getTextFromPage(page)).append('\n');
            }
            return sb.toString().replace('\u00A0', ' ').replaceAll("[ \\t]+", " ");
        } finally {
            reader.close();
        }
    }

    @Test
    void produit_un_pdf() {
        byte[] pdf = rendu.rendre(contenu(Optional.empty()));

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void l_ordre_de_virement_est_rempli_avec_le_titulaire_le_compte_de_cic_et_les_mensualites() throws IOException {
        String texte = texte(rendu.rendre(contenu(Optional.empty())));

        assertThat(texte).contains("AUTORISATION DE PRÉLÈVEMENT", "Ordre de virement permanent",
                "Contrat n° CIC-2026-000001", "Mohamed Ould Ahmed", "1234567890", "22123456",
                "CIC SARL", "BMCI", "00020 00101 00001234567 53",
                "12 480 MRU", "mensuelle, 6 virements, du 25/11/2026 au 25/04/2027", "19/11/2026",
                "Signature du titulaire du compte", "Cachet et visa de la banque");
        for (String date : List.of("25/11/2026", "25/12/2026", "25/01/2027", "25/02/2027", "25/03/2027",
                "25/04/2027")) {
            assertThat(texte).contains(date);
        }
        assertThat(texte.split("2 080 MRU", -1)).hasSize(7);
        assertThat(texte).contains("CIC-2026-000001 M1/6", "CIC-2026-000001 M6/6");
    }

    @Test
    void tient_sur_une_page() throws IOException {
        PdfReader reader = new PdfReader(rendu.rendre(contenu(Optional.empty())));
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
        } finally {
            reader.close();
        }
    }

    @Test
    void ni_prix_d_achat_ni_marge_sur_l_autorisation() throws IOException {
        String texte = texte(rendu.rendre(contenu(Optional.empty())));

        assertThat(texte).doesNotContain("18 400", "10 080", "2 400", "20 000");
    }

    @Test
    void la_banque_du_client_est_remplie_quand_elle_est_connue() throws IOException {
        String texte = texte(rendu.rendre(contenu(Optional.of("BMCI"))));

        assertThat(texte).contains("À l'attention de la banque : BMCI");
    }

    @Test
    void seul_le_virement_permanent_est_mis_en_page() {
        Contrat contrat = exemple();
        AutorisationPrelevement retenue = AutorisationPrelevement.reconstituer(UUID.randomUUID(), contrat.id(),
                contrat.demandeId(), contrat.clientId(), TypeAutorisation.RETENUE_EMPLOYEUR,
                StatutAutorisation.A_SIGNER, ETABLI_LE, ETABLI_LE, null);

        assertThatThrownBy(() -> rendu.rendre(contenu(contrat, retenue, Optional.empty())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
