package com.seheli.identite.adapter.in.web;

import static com.seheli.identite.adapter.in.web.EmploiFormulaireTest.cdiValide;
import static com.seheli.identite.adapter.in.web.EmploiFormulaireTest.formulaire;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.commun.domain.Montant;
import com.seheli.identite.application.ClientsEnMemoire;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.application.EmploisEnMemoire;
import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.Emploi;
import com.seheli.identite.domain.TypeContrat;

class EmploiClientControllerTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-11T09:00:00Z");
    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final EmploisEnMemoire emplois = new EmploisEnMemoire();
    private final EmploiClientController controller = new EmploiClientController(
            new EmploiClientService(clients, emplois, clock), clock);
    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Sidi", LocalDate.of(1990, 1, 15),
            "22123456", MAINTENANT);
    private final Principal principal = () -> client.id().toString();

    EmploiClientControllerTest() {
        clients.enregistrer(client);
    }

    private String enregistrer(EmploiFormulaire saisie, BindingResult erreurs, RedirectAttributesModelMap redirection) {
        return controller.enregistrer(principal, saisie, erreurs, new ExtendedModelMap(), redirection);
    }

    @Test
    void affiche_un_formulaire_vide_sans_emploi() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.formulaire(principal, model)).isEqualTo("client/emploi");
        assertThat(model).containsEntry("existant", false).containsEntry("aujourdhui", "2026-11-11");
        assertThat(((EmploiFormulaire) model.get("formulaire")).getEmployeur()).isEmpty();
    }

    @Test
    void enregistre_puis_modifie_l_emploi_du_client_connecte() {
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();
        EmploiFormulaire saisie = cdiValide();

        assertThat(enregistrer(saisie, new BeanPropertyBindingResult(saisie, "formulaire"), redirection))
                .isEqualTo("redirect:/client/emploi");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("client.emploi.enregistre");
        assertThat(emplois.parClient(client.id())).map(Emploi::employeur).contains("SNIM");

        ExtendedModelMap model = new ExtendedModelMap();
        controller.formulaire(principal, model);
        assertThat(model).containsEntry("existant", true);
        assertThat(((EmploiFormulaire) model.get("formulaire")).getSalaireNet()).isEqualTo("30000");

        EmploiFormulaire modification = formulaire("CDD", "2025-01-01", "2027-12-31", "35000", "28", true, false,
                "VIREMENT_PERMANENT");
        RedirectAttributesModelMap redirection2 = new RedirectAttributesModelMap();
        assertThat(enregistrer(modification, new BeanPropertyBindingResult(modification, "formulaire"), redirection2))
                .isEqualTo("redirect:/client/emploi");
        assertThat(redirection2.getFlashAttributes().get("succes")).isEqualTo("client.emploi.modifie");
        Emploi lu = emplois.parClient(client.id()).orElseThrow();
        assertThat(lu.typeContrat()).isEqualTo(TypeContrat.CDD);
        assertThat(lu.salaireNet()).isEqualTo(Montant.de(35_000));
        assertThat(lu.jourPaie()).isEqualTo(28);
    }

    @Test
    void reaffiche_le_formulaire_si_la_saisie_est_invalide_ou_l_emploi_termine() {
        EmploiFormulaire invalide = formulaire("CDI", "2022-03-01", "", "abc", "25", true, true, "DOMICILIATION_BMCI");
        BindingResult erreurs = new BeanPropertyBindingResult(invalide, "formulaire");
        assertThat(enregistrer(invalide, erreurs, new RedirectAttributesModelMap())).isEqualTo("client/emploi");
        assertThat(erreurs.getFieldError("salaireNet")).isNotNull();

        EmploiFormulaire termine = formulaire("CDD", "2024-01-01", "2026-11-10", "30000", "25", true, false,
                "VIREMENT_PERMANENT");
        BindingResult erreursTermine = new BeanPropertyBindingResult(termine, "formulaire");
        assertThat(enregistrer(termine, erreursTermine, new RedirectAttributesModelMap())).isEqualTo("client/emploi");
        assertThat(erreursTermine.getFieldError("dateFin").getCode()).isEqualTo("client.emploi.date-fin.passee");

        EmploiFormulaire futur = formulaire("CDI", "2026-12-01", "", "30000", "25", true, false, "VIREMENT_PERMANENT");
        BindingResult erreursFutur = new BeanPropertyBindingResult(futur, "formulaire");
        assertThat(enregistrer(futur, erreursFutur, new RedirectAttributesModelMap())).isEqualTo("client/emploi");
        assertThat(erreursFutur.getFieldError("dateDebut").getCode()).isEqualTo("client.emploi.date-debut.future");

        assertThat(emplois.parClient(client.id())).isEmpty();
    }
}
