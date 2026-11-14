package com.seheli.comptabilite.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.comptabilite.application.ApportCapitalService;
import com.seheli.comptabilite.application.ApportsCapitalEnMemoire;
import com.seheli.comptabilite.application.EcrituresEnMemoire;
import com.seheli.comptabilite.application.SaisieApportCapital;
import com.seheli.comptabilite.domain.ApportCapitalInvalideException;

class CapitalBackOfficeControllerTest {

    private static final Principal ADMIN = () -> "admin";

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Africa/Nouakchott"));
    private final ApportCapitalService service = new ApportCapitalService(new ApportsCapitalEnMemoire(),
            new EcrituresEnMemoire(), new JournalAuditService(new JournalEnMemoire()), clock);
    private final CapitalBackOfficeController controller = new CapitalBackOfficeController(service, clock);

    private static MockHttpServletRequest requete(String role) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.addUserRole(role);
        return r;
    }

    @Test
    void l_admin_saisit_un_apport_qui_apparait_dans_la_liste() {
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.saisir("Sidi", "250 000", "2026-11-10", ADMIN, redirection);

        assertThat(vue).isEqualTo("redirect:/bo/capital");
        assertThat(redirection.getFlashAttributes().get("enregistre")).isInstanceOfSatisfying(VueApportCapital.class,
                a -> assertThat(a.verseLe()).isEqualTo("10/11/2026"));
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.page(requete("ADMIN"), model)).isEqualTo("bo/capital/page");
        assertThat(model.get("peutSaisir")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        List<VueApportCapital> apports = (List<VueApportCapital>) model.get("apports");
        assertThat(apports).singleElement().satisfies(a -> {
            assertThat(a.associe()).isEqualTo("Sidi");
            assertThat(a.saisiPar()).isEqualTo("admin");
            assertThat(a.saisiLe()).isEqualTo("12/11/2026 10:00");
        });
    }

    @Test
    void une_saisie_refusee_garde_les_valeurs_tapees_et_l_associe_ne_voit_pas_le_formulaire() {
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        controller.saisir("Sidi", "0", "2026-11-10", ADMIN, redirection);

        assertThat(redirection.getFlashAttributes().get("erreur"))
                .isEqualTo(ApportCapitalInvalideException.MONTANT_INVALIDE);
        assertThat(redirection.getFlashAttributes().get("saisie"))
                .isEqualTo(new SaisieApportCapital("Sidi", "0", "2026-11-10"));
        ExtendedModelMap model = new ExtendedModelMap();
        controller.page(requete("ASSOCIE"), model);
        assertThat(model.get("peutSaisir")).isEqualTo(false);
        assertThat(model.get("apports")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).isEmpty();
    }
}
