package com.cic.demonstration.adapter.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cic.demonstration.application.ReinitialisationDemonstrationService;
import com.cic.demonstration.domain.ReinitialisationEnCoursException;

/**
 * Profil demo, rôle ADMIN (voir {@code SecurityConfig}) : bouton « Réinitialiser la démo ». Une page de
 * confirmation précède l'effacement, qui ne se déclenche que par un POST protégé par CSRF.
 */
@Controller
@Profile("demo")
@RequestMapping("/bo/demonstration/reinitialisation")
class ReinitialisationDemonstrationController {

    private static final Logger LOG = LoggerFactory.getLogger(ReinitialisationDemonstrationController.class);

    private final ReinitialisationDemonstrationService reinitialisation;

    ReinitialisationDemonstrationController(ReinitialisationDemonstrationService reinitialisation) {
        this.reinitialisation = reinitialisation;
    }

    @GetMapping
    String confirmation() {
        return "bo/demonstration/reinitialisation";
    }

    @PostMapping
    String reinitialiser(RedirectAttributes redirection) {
        try {
            boolean chargees = reinitialisation.reinitialiser();
            redirection.addFlashAttribute(chargees ? "succes" : "erreur",
                    chargees ? "bo.demonstration.reinitialisee" : "bo.demonstration.chargement-interrompu");
        } catch (ReinitialisationEnCoursException e) {
            redirection.addFlashAttribute("erreur", "bo.demonstration.deja-en-cours");
        } catch (RuntimeException e) {
            LOG.error("Réinitialisation de la démonstration en échec", e);
            redirection.addFlashAttribute("erreur", "bo.demonstration.echec");
        }
        return "redirect:/bo";
    }
}
