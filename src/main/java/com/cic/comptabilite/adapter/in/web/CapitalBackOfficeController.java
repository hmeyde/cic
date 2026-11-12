package com.cic.comptabilite.adapter.in.web;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cic.commun.domain.ErreurMetier;
import com.cic.comptabilite.application.ApportCapitalService;
import com.cic.comptabilite.application.SaisieApportCapital;
import com.cic.comptabilite.domain.ApportCapital;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Back-office : apports en capital des associés (voir {@code SecurityConfig}). La liste est visible
 * des rôles ADMIN et ASSOCIE ; la saisie est réservée au rôle ADMIN. Après une saisie, redirection
 * vers la page (pas de double envoi au rechargement) ; une saisie refusée garde les valeurs tapées
 * (attributs flash, jamais dans l'URL). L'auteur est l'utilisateur connecté.
 */
@Controller
class CapitalBackOfficeController {

    static final String CHEMIN = "/bo/capital";
    static final String ROLE_SAISIE = "ADMIN";

    private static final Logger LOG = LoggerFactory.getLogger(CapitalBackOfficeController.class);

    private final ApportCapitalService apports;
    private final Clock clock;

    CapitalBackOfficeController(ApportCapitalService apports, Clock clock) {
        this.apports = apports;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String page(HttpServletRequest requete, Model model) {
        model.addAttribute("peutSaisir", requete.isUserInRole(ROLE_SAISIE));
        model.addAttribute("apports", apports.tous().stream().map(a -> VueApportCapital.depuis(a, clock)).toList());
        if (!model.containsAttribute("saisie")) {
            model.addAttribute("saisie", new SaisieApportCapital("", "", LocalDate.now(clock).toString()));
        }
        model.addAttribute("aujourdhui", LocalDate.now(clock).toString());
        model.addAttribute("longueurAssocieMax", ApportCapital.ASSOCIE_LONGUEUR_MAX);
        return "bo/capital/page";
    }

    @PostMapping(CHEMIN)
    String saisir(@RequestParam(name = "associe", required = false) String associe,
            @RequestParam(name = "montant", required = false) String montant,
            @RequestParam(name = "verseLe", required = false) String verseLe, Principal auteur,
            RedirectAttributes redirection) {
        SaisieApportCapital saisie = new SaisieApportCapital(associe, montant, verseLe);
        try {
            ApportCapital a = apports.saisir(saisie, auteur.getName());
            redirection.addFlashAttribute("enregistre", VueApportCapital.depuis(a, clock));
        } catch (ErreurMetier e) {
            LOG.debug("Saisie d'apport en capital refusée : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
            redirection.addFlashAttribute("saisie", saisie);
        }
        return "redirect:" + CHEMIN;
    }
}
