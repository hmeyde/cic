package com.seheli.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Page de démonstration des composants du design system, et déclenchement volontaire d'une erreur
 * serveur pour voir la page 500 à la marque Seheli.
 * Disponible uniquement en profil dev.
 */
@Profile("dev")
@Controller
class ComposantsDevController {

    @GetMapping("/dev/composants")
    String composants() {
        return "dev/composants";
    }

    @GetMapping("/dev/erreur-serveur")
    String erreurServeur() {
        throw new IllegalStateException("Erreur volontaire (profil dev) : affichage de la page 500");
    }
}
