package com.cic.demande.adapter.in.web;

import java.security.Principal;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.cic.commun.domain.ErreurMetier;
import com.cic.demande.application.DecisionService;
import com.cic.demande.application.DossierIntrouvableException;
import com.cic.identite.domain.TypeDocument;

/**
 * Back-office : décision de l'analyste sur un dossier depuis sa fiche (accepter, refuser avec un
 * motif, demander un justificatif), rôle ANALYSTE (voir {@code SecurityConfig}). L'auteur est
 * l'utilisateur connecté ; le motif voyage dans le corps de la requête, jamais dans l'URL. Après
 * chaque action, retour sur la fiche avec un message.
 */
@Controller
class DecisionBackOfficeController {

    static final String CHEMIN_ACCEPTATION = FicheDossierBackOfficeController.CHEMIN + "/acceptation";
    static final String CHEMIN_REFUS = FicheDossierBackOfficeController.CHEMIN + "/refus";
    static final String CHEMIN_JUSTIFICATIF = FicheDossierBackOfficeController.CHEMIN + "/justificatif";

    private static final Logger LOG = LoggerFactory.getLogger(DecisionBackOfficeController.class);

    private final DecisionService decisions;

    DecisionBackOfficeController(DecisionService decisions) {
        this.decisions = decisions;
    }

    @PostMapping(CHEMIN_ACCEPTATION)
    String accepter(@PathVariable("id") UUID id, Principal analyste, RedirectAttributes redirection) {
        return decider(id, redirection, "bo.decision.acceptee",
                () -> decisions.accepter(id, analyste.getName()));
    }

    @PostMapping(CHEMIN_REFUS)
    String refuser(@PathVariable("id") UUID id, @RequestParam(name = "motif", required = false) String motif,
            Principal analyste, RedirectAttributes redirection) {
        return decider(id, redirection, "bo.decision.refusee",
                () -> decisions.refuser(id, analyste.getName(), motif));
    }

    @PostMapping(CHEMIN_JUSTIFICATIF)
    String demanderJustificatif(@PathVariable("id") UUID id, @RequestParam("type") TypeDocument type,
            @RequestParam(name = "precision", required = false) String precision, Principal analyste,
            RedirectAttributes redirection) {
        return decider(id, redirection, "bo.decision.justificatif-demande",
                () -> decisions.demanderJustificatif(id, analyste.getName(), type, precision));
    }

    private static String decider(UUID id, RedirectAttributes redirection, String succes, Supplier<?> decision) {
        try {
            decision.get();
            redirection.addFlashAttribute("succes", succes);
        } catch (DossierIntrouvableException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (ErreurMetier e) {
            LOG.debug("Décision non enregistrée : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
        }
        return "redirect:" + UriComponentsBuilder.fromPath(FicheDossierBackOfficeController.CHEMIN)
                .buildAndExpand(id).toUriString();
    }
}
