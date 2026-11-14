package com.seheli.identite.adapter.in.web;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.seheli.identite.application.ContratTermineException;
import com.seheli.identite.application.DateDebutEmploiFutureException;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeContrat;

/**
 * Écran emploi du client connecté : déclaration puis modification de son emploi actuel. Le client est
 * celui de la session (principal = identifiant du client), jamais un identifiant passé dans l'URL.
 */
@Controller
class EmploiClientController {

    static final String CHEMIN = "/client/emploi";
    static final String VUE = "client/emploi";

    private final EmploiClientService emplois;
    private final Clock clock;

    EmploiClientController(EmploiClientService emplois, Clock clock) {
        this.emplois = emplois;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String formulaire(Principal principal, Model model) {
        Optional<EmploiFormulaire> existant = emplois.parClient(clientId(principal)).map(EmploiFormulaire::depuis);
        return vue(model, existant.orElseGet(EmploiFormulaire::new), existant.isPresent());
    }

    @PostMapping(CHEMIN)
    String enregistrer(Principal principal, @ModelAttribute("formulaire") EmploiFormulaire formulaire,
            BindingResult erreurs, Model model, RedirectAttributes redirection) {
        UUID clientId = clientId(principal);
        boolean existant = emplois.parClient(clientId).isPresent();
        formulaire.valider(erreurs);
        if (!erreurs.hasErrors()) {
            try {
                emplois.enregistrer(clientId, formulaire.versCommande());
                redirection.addFlashAttribute("succes",
                        existant ? "client.emploi.modifie" : "client.emploi.enregistre");
                return "redirect:" + CHEMIN;
            } catch (DateDebutEmploiFutureException e) {
                erreurs.rejectValue("dateDebut", e.code());
            } catch (ContratTermineException e) {
                erreurs.rejectValue("dateFin", e.code());
            }
        }
        return vue(model, formulaire, existant);
    }

    private String vue(Model model, EmploiFormulaire formulaire, boolean existant) {
        model.addAttribute("formulaire", formulaire);
        model.addAttribute("existant", existant);
        model.addAttribute("typesContrat", TypeContrat.values());
        model.addAttribute("modesRemboursement", ModeRemboursement.values());
        model.addAttribute("aujourdhui", LocalDate.now(clock).toString());
        return VUE;
    }

    private static UUID clientId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
