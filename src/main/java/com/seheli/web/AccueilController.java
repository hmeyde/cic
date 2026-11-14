package com.seheli.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Page d'accueil publique, avec les balises Open Graph (aperçu du lien partagé : titre, description,
 * logo). Open Graph exige des URL absolues : elles sont construites depuis la requête courante.
 */
@Controller
class AccueilController {

    @GetMapping("/")
    String accueil(Model model) {
        model.addAttribute("ogTitre", "accueil.titre-page");
        model.addAttribute("ogUrl", ServletUriComponentsBuilder.fromCurrentContextPath().path("/").toUriString());
        model.addAttribute("ogImage",
                ServletUriComponentsBuilder.fromCurrentContextPath().path("/img/og-seheli.png").toUriString());
        return "accueil";
    }
}
