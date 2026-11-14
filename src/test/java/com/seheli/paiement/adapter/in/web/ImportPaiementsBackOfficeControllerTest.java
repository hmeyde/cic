package com.seheli.paiement.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.contrat.application.AutorisationsEnMemoire;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.ContratsEnMemoire;
import com.seheli.paiement.application.FichierCsvInvalideException;
import com.seheli.paiement.application.ImportPaiementsService;
import com.seheli.paiement.application.PaiementService;
import com.seheli.paiement.application.PaiementsEnMemoire;
import com.seheli.paiement.domain.TypeEcart;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class ImportPaiementsBackOfficeControllerTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-14T10:00:00Z");
    private static final Principal ADMIN = () -> "admin";

    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final ImportPaiementsBackOfficeController controller;

    ImportPaiementsBackOfficeControllerTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        ContratService contrats = new ContratService(new ContratsEnMemoire(), new AutorisationsEnMemoire(), parametres,
                clock);
        PaiementService paiements = new PaiementService(new PaiementsEnMemoire(), contrats,
                new JournalAuditService(new JournalEnMemoire()), evenement -> { }, clock);
        controller = new ImportPaiementsBackOfficeController(new ImportPaiementsService(paiements), paiements, clock);
    }

    private RedirectAttributesModelMap importer(String csv) {
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();
        MockMultipartFile fichier = new MockMultipartFile("fichier", "paiements.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        assertThat(controller.importer(fichier, ADMIN, redirection)).isEqualTo("redirect:/bo/paiements/import");
        return redirection;
    }

    @Test
    void le_fichier_importe_donne_un_bilan_et_ses_ecarts_sont_listes() {
        RedirectAttributesModelMap r = importer("reference;montant;date\nINCONNUE123;5000;2026-11-12\n;1;x\n");

        assertThat(r.getFlashAttributes().get("resultat")).isInstanceOfSatisfying(VueResultatImport.class, v -> {
            assertThat(v.lignes()).isEqualTo(2);
            assertThat(v.enregistres()).isEqualTo(1);
            assertThat(v.invalides()).isEqualTo(1);
            assertThat(v.ecarts()).singleElement().satisfies(l -> {
                assertThat(l.numero()).isEqualTo(2);
                assertThat(l.paiement()).hasValueSatisfying(
                        p -> assertThat(p.ecart()).contains(TypeEcart.REFERENCE_INCONNUE));
            });
        });

        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.ecarts(model)).isEqualTo("bo/paiements/ecarts");
        assertThat((List<?>) model.get("ecarts")).singleElement().isInstanceOfSatisfying(VuePaiement.class,
                p -> assertThat(p.reference()).isEqualTo("INCONNUE123"));
    }

    @Test
    void un_fichier_absent_ou_vide_est_refuse() {
        RedirectAttributesModelMap r = new RedirectAttributesModelMap();
        controller.importer(null, ADMIN, r);
        assertThat(r.getFlashAttributes().get("erreur")).isEqualTo(FichierCsvInvalideException.VIDE);

        assertThat(importer("reference;montant;date\n").getFlashAttributes().get("erreur"))
                .isEqualTo(FichierCsvInvalideException.VIDE);
    }
}
