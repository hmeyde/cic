package com.seheli.paiement.adapter.in.web;

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

import com.seheli.commun.domain.ErreurMetier;
import com.seheli.paiement.application.PaiementService;
import com.seheli.paiement.application.SaisiePaiement;
import com.seheli.paiement.domain.Paiement;
import com.seheli.paiement.domain.PaiementEnDoubleException;
import com.seheli.paiement.domain.ReferencePaiement;

/**
 * Back-office : saisie manuelle des paiements reçus et rapprochement par référence, rôles
 * RECOUVREMENT et ADMIN (voir {@code SecurityConfig}). La page montre le formulaire de saisie et les
 * derniers paiements saisis. Après une saisie, redirection vers la page (pas de double envoi au
 * rechargement) avec le résultat du rapprochement ; une saisie refusée garde les valeurs tapées
 * (attributs flash, en session, jamais dans l'URL). L'auteur est l'utilisateur connecté.
 */
@Controller
class PaiementsBackOfficeController {

    static final String CHEMIN = "/bo/paiements";
    static final int LIMITE = 100;

    private static final Logger LOG = LoggerFactory.getLogger(PaiementsBackOfficeController.class);

    private final PaiementService paiements;
    private final Clock clock;

    PaiementsBackOfficeController(PaiementService paiements, Clock clock) {
        this.paiements = paiements;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String page(Model model) {
        model.addAttribute("paiements", paiements.derniers(LIMITE).stream().map(p -> VuePaiement.depuis(p, clock))
                .toList());
        if (!model.containsAttribute("saisie")) {
            model.addAttribute("saisie", new SaisiePaiement("", "", LocalDate.now(clock).toString()));
        }
        model.addAttribute("aujourdhui", LocalDate.now(clock).toString());
        model.addAttribute("longueurReferenceMax", ReferencePaiement.LONGUEUR_MAX);
        model.addAttribute("limite", LIMITE);
        return "bo/paiements/page";
    }

    @PostMapping(CHEMIN)
    String saisir(@RequestParam(name = "reference", required = false) String reference,
            @RequestParam(name = "montant", required = false) String montant,
            @RequestParam(name = "recuLe", required = false) String recuLe, Principal auteur,
            RedirectAttributes redirection) {
        SaisiePaiement saisie = new SaisiePaiement(reference, montant, recuLe);
        try {
            Paiement p = paiements.saisir(saisie, auteur.getName());
            redirection.addFlashAttribute("resultat", VuePaiement.depuis(p, clock));
        } catch (PaiementEnDoubleException e) {
            LOG.debug("Paiement en double refusé");
            redirection.addFlashAttribute("erreur", e.code());
            e.paiementExistantId().flatMap(paiements::parId)
                    .ifPresent(p -> redirection.addFlashAttribute("existant", VuePaiement.depuis(p, clock)));
            redirection.addFlashAttribute("saisie", saisie);
        } catch (ErreurMetier e) {
            LOG.debug("Saisie de paiement refusée : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
            redirection.addFlashAttribute("saisie", saisie);
        }
        return "redirect:" + CHEMIN;
    }
}
