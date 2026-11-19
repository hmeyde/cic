package com.seheli.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Page de démonstration des composants du design system.
 * Disponible uniquement en profil dev.
 */
@Profile("dev")
@Controller
class ComposantsDevController {

    @GetMapping("/dev/composants")
    String composants() {
        return "dev/composants";
    }
}
