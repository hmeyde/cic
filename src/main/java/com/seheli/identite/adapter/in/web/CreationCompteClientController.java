package com.seheli.identite.adapter.in.web;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.seheli.identite.application.AgeMinimumNonAtteintException;
import com.seheli.identite.application.CompteDejaExistantException;
import com.seheli.identite.application.ConsentementManquantException;
import com.seheli.identite.application.CreationCompteClientService;
import com.seheli.identite.application.DateNaissanceInvalideException;
import com.seheli.identite.application.NniDejaUtiliseException;
import com.seheli.identite.domain.Client;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Création du compte d'un nouveau client : NNI, nom, prénom, date de naissance et consentement explicite.
 *
 * <p>Accessible seulement après un code de connexion valide pour un téléphone sans compte : ce téléphone,
 * gardé en session par {@link ConnexionClientController}, devient celui du compte. Sans lui, retour à la
 * connexion. Une fois le compte créé, la session client est ouverte.
 */
@Controller
class CreationCompteClientController {

    static final String CHEMIN = "/client/connexion/compte";
    static final String VUE = "client/creation-compte";

    private final CreationCompteClientService creation;
    private final SessionClient sessionClient;
    private final Clock clock;

    CreationCompteClientController(CreationCompteClientService creation, SessionClient sessionClient, Clock clock) {
        this.creation = creation;
        this.sessionClient = sessionClient;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String formulaire(HttpSession session, Model model) {
        if (telephoneVerifie(session) == null) {
            return "redirect:/client/connexion";
        }
        return vue(model, new CompteClientFormulaire(), telephoneVerifie(session));
    }

    @PostMapping(CHEMIN)
    String creer(@ModelAttribute("formulaire") CompteClientFormulaire formulaire, BindingResult erreurs,
            HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model,
            RedirectAttributes redirection) {
        String telephone = telephoneVerifie(session);
        if (telephone == null) {
            return "redirect:/client/connexion";
        }
        formulaire.valider(erreurs);
        if (!erreurs.hasErrors()) {
            try {
                Client client = creation.creer(formulaire.versCommande(telephone));
                session.removeAttribute(ConnexionClientController.SESSION_TELEPHONE_VERIFIE);
                sessionClient.ouvrir(client.id(), request, response);
                redirection.addFlashAttribute("succes", "client.compte.cree");
                // Étape suivante du parcours de demande : l'emploi.
                return "redirect:" + EmploiClientController.CHEMIN;
            } catch (ConsentementManquantException e) {
                erreurs.rejectValue("consentement", e.code());
            } catch (DateNaissanceInvalideException e) {
                erreurs.rejectValue("dateNaissance", e.code());
            } catch (AgeMinimumNonAtteintException e) {
                erreurs.rejectValue("dateNaissance", e.code(), new Object[] {e.ageMinimum()}, null);
            } catch (NniDejaUtiliseException e) {
                erreurs.rejectValue("nni", e.code());
            } catch (CompteDejaExistantException e) {
                // Compte créé entre-temps pour ce téléphone (autre onglet) : il suffit de se connecter.
                session.removeAttribute(ConnexionClientController.SESSION_TELEPHONE_VERIFIE);
                redirection.addFlashAttribute("erreur", e.code());
                return "redirect:/client/connexion";
            }
        }
        return vue(model, formulaire, telephone);
    }

    private String vue(Model model, CompteClientFormulaire formulaire, String telephone) {
        model.addAttribute("formulaire", formulaire);
        model.addAttribute("telephoneMasque", ConnexionClientController.masquer(telephone));
        model.addAttribute("dateMax", LocalDate.now(clock).toString());
        return VUE;
    }

    private static String telephoneVerifie(HttpSession session) {
        return (String) session.getAttribute(ConnexionClientController.SESSION_TELEPHONE_VERIFIE);
    }
}
