package com.seheli.paiement.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.AutorisationsEnMemoire;
import com.seheli.contrat.application.ContratAEtablir;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.ContratsEnMemoire;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Prix;
import com.seheli.paiement.application.PaiementService;
import com.seheli.paiement.application.PaiementsEnMemoire;
import com.seheli.paiement.application.SaisiePaiement;
import com.seheli.paiement.domain.PaiementEnDoubleException;
import com.seheli.paiement.domain.ReferenceApport;
import com.seheli.paiement.domain.StatutRapprochement;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class PaiementsBackOfficeControllerTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant MAINTENANT = Instant.parse("2026-11-14T10:00:00Z");
    private static final Principal ADMIN = () -> "admin";

    private final Clock clock = Clock.fixed(MAINTENANT, NOUAKCHOTT);
    private final PaiementsEnMemoire paiements = new PaiementsEnMemoire();
    private final ContratService contrats;
    private final PaiementsBackOfficeController controller;

    PaiementsBackOfficeControllerTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        contrats = new ContratService(new ContratsEnMemoire(), new AutorisationsEnMemoire(), parametres, clock);
        controller = new PaiementsBackOfficeController(new PaiementService(paiements, contrats,
                new JournalAuditService(new JournalEnMemoire()), evenement -> { }, clock), clock);
    }

    private Contrat contrat() {
        return contrats.etablir(new ContratAEtablir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), 6, 25, Optional.empty(), MAINTENANT.minusSeconds(86_400)));
    }

    private RedirectAttributesModelMap saisir(String reference, String montant, String recuLe) {
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();
        assertThat(controller.saisir(reference, montant, recuLe, ADMIN, redirection)).isEqualTo("redirect:/bo/paiements");
        return redirection;
    }

    @Test
    void le_paiement_saisi_est_rattache_au_dossier_et_affiche() {
        Contrat c = contrat();
        String reference = ReferenceApport.pourContrat(c.numero()).valeur();

        RedirectAttributesModelMap r = saisir(reference, "8 320", "2026-11-14");

        assertThat(r.getFlashAttributes().get("resultat")).isInstanceOfSatisfying(VuePaiement.class, v -> {
            assertThat(v.statut()).isEqualTo(StatutRapprochement.RAPPROCHE);
            assertThat(v.numeroContrat()).contains(c.numero());
            assertThat(v.recuLe()).isEqualTo("14/11/2026");
        });
        assertThat(r.getFlashAttributes()).doesNotContainKey("erreur");
        assertThat(paiements.tous()).singleElement()
                .satisfies(p -> assertThat(p.rattachement().orElseThrow().demandeId()).isEqualTo(c.demandeId()));

        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.page(model)).isEqualTo("bo/paiements/page");
        assertThat((List<?>) model.get("paiements")).singleElement().isInstanceOfSatisfying(VuePaiement.class,
                v -> assertThat(v.reference()).isEqualTo(reference));
    }

    @Test
    void un_doublon_est_refuse_et_le_paiement_existant_montre() {
        Contrat c = contrat();
        String reference = ReferenceApport.pourContrat(c.numero()).valeur();
        saisir(reference, "8320", "2026-11-14");

        RedirectAttributesModelMap r = saisir(reference, "8320", "2026-11-14");

        assertThat(r.getFlashAttributes().get("erreur")).isEqualTo(PaiementEnDoubleException.CODE);
        assertThat(r.getFlashAttributes().get("existant")).isInstanceOfSatisfying(VuePaiement.class,
                v -> assertThat(v.reference()).isEqualTo(reference));
        assertThat(r.getFlashAttributes()).doesNotContainKey("resultat");
        assertThat(paiements.tous()).hasSize(1);
    }

    @Test
    void une_saisie_invalide_garde_les_valeurs_tapees() {
        RedirectAttributesModelMap r = saisir("AP2026", "abc", "2026-11-14");

        assertThat(r.getFlashAttributes().get("erreur")).isEqualTo("bo.paiement.montant.invalide");
        assertThat(r.getFlashAttributes().get("saisie")).isEqualTo(new SaisiePaiement("AP2026", "abc", "2026-11-14"));
        assertThat(paiements.tous()).isEmpty();
    }

    @Test
    void le_formulaire_propose_la_date_du_jour() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.page(model);

        assertThat(model.get("saisie")).isEqualTo(new SaisiePaiement("", "", "2026-11-14"));
    }
}
