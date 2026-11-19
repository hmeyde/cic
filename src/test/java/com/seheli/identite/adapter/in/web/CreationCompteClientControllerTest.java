package com.seheli.identite.adapter.in.web;

import static com.seheli.identite.adapter.in.web.CompteClientFormulaireTest.formulaire;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.identite.application.ClientsEnMemoire;
import com.seheli.identite.application.CreationCompteClientService;
import com.seheli.identite.domain.Client;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class CreationCompteClientControllerTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-11T09:00:00Z");
    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final CreationCompteClientController controller = new CreationCompteClientController(
            new CreationCompteClientService(clients, parametres(), clock), new SessionClient(), clock);

    private final MockHttpSession session = new MockHttpSession();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    CreationCompteClientControllerTest() {
        request.setSession(session);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static ParametreService parametres() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        return parametres;
    }

    private void telephoneProuve() {
        session.setAttribute(ConnexionClientController.SESSION_TELEPHONE_VERIFIE, "22123456");
    }

    private String creer(CompteClientFormulaire saisie, BindingResult erreurs, ExtendedModelMap model) {
        return controller.creer(saisie, erreurs, request, response, session, model, new RedirectAttributesModelMap());
    }

    @Test
    void sans_telephone_prouve_retour_a_la_connexion() {
        assertThat(controller.formulaire(session, new ExtendedModelMap())).isEqualTo("redirect:/client/connexion");
        CompteClientFormulaire saisie = formulaire("1234567890", "Ould Ahmed", "Sidi", "1990-01-15", true);
        assertThat(creer(saisie, new BeanPropertyBindingResult(saisie, "formulaire"), new ExtendedModelMap()))
                .isEqualTo("redirect:/client/connexion");
        assertThat(clients.parNni("1234567890")).isEmpty();
    }

    @Test
    void affiche_le_formulaire_avec_le_telephone_masque() {
        telephoneProuve();
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.formulaire(session, model)).isEqualTo("client/creation-compte");
        assertThat(model).containsEntry("telephoneMasque", "****3456").containsEntry("dateMax", "2026-11-11");
    }

    @Test
    void cree_le_compte_puis_ouvre_la_session_du_client() {
        telephoneProuve();
        CompteClientFormulaire saisie = formulaire("1234567890", "Ould Ahmed", "Sidi", "1990-01-15", true);

        String vue = creer(saisie, new BeanPropertyBindingResult(saisie, "formulaire"), new ExtendedModelMap());

        assertThat(vue).isEqualTo("redirect:/client/emploi");
        Client client = clients.parTelephone("22123456").orElseThrow();
        assertThat(client.consentementDonneesLe()).isEqualTo(MAINTENANT);
        assertThat(session.getAttribute(ConnexionClientController.SESSION_TELEPHONE_VERIFIE)).isNull();
        SecurityContext contexte = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        Authentication authentification = contexte.getAuthentication();
        assertThat(authentification.getName()).isEqualTo(client.id().toString());
        assertThat(authentification.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    void reaffiche_le_formulaire_sans_consentement() {
        telephoneProuve();
        CompteClientFormulaire saisie = formulaire("1234567890", "Ould Ahmed", "Sidi", "1990-01-15", false);
        BindingResult erreurs = new BeanPropertyBindingResult(saisie, "formulaire");

        assertThat(creer(saisie, erreurs, new ExtendedModelMap())).isEqualTo("client/creation-compte");
        assertThat(erreurs.getFieldError("consentement").getCode()).isEqualTo("client.compte.consentement.manquant");
        assertThat(clients.parTelephone("22123456")).isEmpty();
    }

    @Test
    void reaffiche_le_formulaire_si_le_client_est_mineur_ou_le_nni_pris() {
        telephoneProuve();
        clients.enregistrer(Client.nouveau("1111111111", "A", "B", LocalDate.of(1980, 1, 1), "33000000", MAINTENANT));

        CompteClientFormulaire mineur = formulaire("1234567890", "Ould Ahmed", "Sidi", "2010-01-15", true);
        BindingResult erreursMineur = new BeanPropertyBindingResult(mineur, "formulaire");
        assertThat(creer(mineur, erreursMineur, new ExtendedModelMap())).isEqualTo("client/creation-compte");
        assertThat(erreursMineur.getFieldError("dateNaissance").getCode()).isEqualTo("client.compte.age.minimum");
        assertThat(erreursMineur.getFieldError("dateNaissance").getArguments()).containsExactly(18);

        CompteClientFormulaire nniPris = formulaire("1111111111", "Ould Ahmed", "Sidi", "1990-01-15", true);
        BindingResult erreursNni = new BeanPropertyBindingResult(nniPris, "formulaire");
        assertThat(creer(nniPris, erreursNni, new ExtendedModelMap())).isEqualTo("client/creation-compte");
        assertThat(erreursNni.getFieldError("nni").getCode()).isEqualTo("client.compte.nni.deja.utilise");
    }
}
