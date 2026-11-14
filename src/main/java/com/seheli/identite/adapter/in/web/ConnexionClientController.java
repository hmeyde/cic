package com.seheli.identite.adapter.in.web;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.seheli.identite.application.ConnexionClientService;
import com.seheli.identite.application.ResultatConnexion;
import com.seheli.identite.application.TelephoneInvalideException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Connexion du client en deux étapes : saisie du téléphone (envoi du code), puis saisie du code.
 *
 * <p>Le téléphone est gardé en session entre les deux étapes, jamais dans l'URL. Un code valide ouvre
 * une session authentifiée avec le rôle CLIENT ({@link SessionClient}) si un compte existe pour ce
 * téléphone ; sinon le téléphone, désormais prouvé, est gardé en session et le nouveau client est
 * envoyé vers la création de son compte ({@link CreationCompteClientController}).
 */
@Controller
class ConnexionClientController {

    static final String SESSION_TELEPHONE = ConnexionClientController.class.getName() + ".telephone";
    /** Téléphone dont la possession a été prouvée par un code valide, sans compte associé. */
    static final String SESSION_TELEPHONE_VERIFIE = ConnexionClientController.class.getName() + ".telephoneVerifie";

    private final ConnexionClientService connexion;
    private final SessionClient sessionClient;

    ConnexionClientController(ConnexionClientService connexion, SessionClient sessionClient) {
        this.connexion = connexion;
        this.sessionClient = sessionClient;
    }

    @GetMapping("/client/connexion")
    String telephone() {
        return "client/connexion";
    }

    @PostMapping("/client/connexion")
    String demanderCode(@RequestParam(defaultValue = "") String telephone, Locale langue, HttpSession session,
            Model model) {
        try {
            String normalise = connexion.demanderCode(telephone, langue);
            session.removeAttribute(SESSION_TELEPHONE_VERIFIE);
            session.setAttribute(SESSION_TELEPHONE, normalise);
            return "redirect:/client/connexion/code";
        } catch (TelephoneInvalideException e) {
            model.addAttribute("erreur", e.code());
            return "client/connexion";
        }
    }

    @GetMapping("/client/connexion/code")
    String code(HttpSession session, Model model) {
        String telephone = (String) session.getAttribute(SESSION_TELEPHONE);
        if (telephone == null) {
            return "redirect:/client/connexion";
        }
        model.addAttribute("telephoneMasque", masquer(telephone));
        return "client/code";
    }

    @PostMapping("/client/connexion/code")
    String verifierCode(@RequestParam(defaultValue = "") String code, HttpServletRequest request,
            HttpServletResponse response, HttpSession session, Model model, RedirectAttributes redirection) {
        String telephone = (String) session.getAttribute(SESSION_TELEPHONE);
        if (telephone == null) {
            return "redirect:/client/connexion";
        }
        ResultatConnexion resultat = connexion.verifierCode(telephone, code);
        if (resultat.estNouveauClient()) {
            session.removeAttribute(SESSION_TELEPHONE);
            // Le téléphone vient d'être prouvé : nouvel identifiant de session avant d'en garder la trace.
            request.changeSessionId();
            session.setAttribute(SESSION_TELEPHONE_VERIFIE, telephone);
            return "redirect:" + CreationCompteClientController.CHEMIN;
        }
        if (resultat.estValide()) {
            session.removeAttribute(SESSION_TELEPHONE);
            sessionClient.ouvrir(resultat.clientId().orElseThrow(), request, response);
            return "redirect:/client/";
        }
        switch (resultat.verification()) {
            case INCORRECT -> {
                model.addAttribute("erreur", "client.connexion.code.incorrect");
                model.addAttribute("essaisRestants", resultat.essaisRestants());
                model.addAttribute("telephoneMasque", masquer(telephone));
                return "client/code";
            }
            default -> {
                // Expiré, essais épuisés ou déjà utilisé : il faut demander un nouveau code.
                session.removeAttribute(SESSION_TELEPHONE);
                redirection.addFlashAttribute("erreur", "client.connexion.code." + resultat.verification());
                return "redirect:/client/connexion";
            }
        }
    }

    /** Seule forme du téléphone affichée : {@code ****3456}. */
    static String masquer(String telephone) {
        return "****" + telephone.substring(telephone.length() - 4);
    }
}
