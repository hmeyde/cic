package com.cic.identite.adapter.in.web;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cic.identite.application.ConnexionClientService;
import com.cic.identite.application.ResultatConnexion;
import com.cic.identite.application.TelephoneInvalideException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Connexion du client en deux étapes : saisie du téléphone (envoi du code), puis saisie du code.
 *
 * <p>Le téléphone est gardé en session entre les deux étapes, jamais dans l'URL. Un code valide ouvre
 * une session authentifiée avec le rôle CLIENT ; le principal est l'identifiant du client (UUID),
 * pas son téléphone. L'identifiant de session est renouvelé à la connexion (fixation de session).
 */
@Controller
class ConnexionClientController {

    static final String ROLE_CLIENT = "ROLE_CLIENT";
    static final String SESSION_TELEPHONE = ConnexionClientController.class.getName() + ".telephone";

    private final ConnexionClientService connexion;
    private final SecurityContextRepository contextes = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy strategie = SecurityContextHolder.getContextHolderStrategy();

    ConnexionClientController(ConnexionClientService connexion) {
        this.connexion = connexion;
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
        if (resultat.estValide()) {
            session.removeAttribute(SESSION_TELEPHONE);
            ouvrirSession(resultat.clientId().orElseThrow(), request, response);
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

    private void ouvrirSession(UUID clientId, HttpServletRequest request, HttpServletResponse response) {
        request.changeSessionId();
        var authentification = UsernamePasswordAuthenticationToken.authenticated(clientId.toString(), null,
                List.of(new SimpleGrantedAuthority(ROLE_CLIENT)));
        SecurityContext contexte = strategie.createEmptyContext();
        contexte.setAuthentication(authentification);
        strategie.setContext(contexte);
        contextes.saveContext(contexte, request, response);
    }

    /** Seule forme du téléphone affichée : {@code ****3456}. */
    static String masquer(String telephone) {
        return "****" + telephone.substring(telephone.length() - 4);
    }
}
