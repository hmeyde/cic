package com.cic.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Page d'accueil publique.
 */
@Controller
class AccueilController {

    @GetMapping("/")
    String accueil() {
        return "accueil";
    }
}
