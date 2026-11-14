package com.seheli.web;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Accueil du back-office, après connexion. */
@Controller
class BackOfficeAccueilController {

    private static final String PREFIXE_ROLE = "ROLE_";

    private final boolean demonstration;

    BackOfficeAccueilController(Environment environment) {
        this.demonstration = environment.matchesProfiles("demo");
    }

    @GetMapping({"/bo", "/bo/"})
    String accueil(Authentication authentication, Model model) {
        model.addAttribute("identifiant", authentication.getName());
        model.addAttribute("role", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith(PREFIXE_ROLE))
                .map(a -> a.substring(PREFIXE_ROLE.length()))
                .findFirst()
                .orElse(""));
        model.addAttribute("demonstration", demonstration);
        return "bo/accueil";
    }
}
