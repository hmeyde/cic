package com.cic.recouvrement.adapter.in.web;

import java.security.Principal;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.cic.commun.domain.ErreurMetier;
import com.cic.demande.application.DossierIntrouvableException;
import com.cic.demande.application.SuiviRemboursementService;
import com.cic.demande.domain.Demande;
import com.cic.recouvrement.application.SuiviRecouvrementService;
import com.cic.recouvrement.application.TableauRecouvrement;
import com.cic.recouvrement.domain.NoteRecouvrement;

/**
 * Back-office : écran du recouvrement, rôles RECOUVREMENT et ADMIN (voir {@code SecurityConfig}).
 *
 * <ul>
 *   <li>{@value #CHEMIN} : prélèvements à venir, réussis et en échec ;</li>
 *   <li>{@value #CHEMIN_FICHE} : échéancier, relances et notes datées d'un dossier, avec les décisions
 *       possibles (régulariser, passer en recouvrement, solder).</li>
 * </ul>
 *
 * Note et motif voyagent dans le corps de la requête, jamais dans l'URL. Après chaque action, retour
 * sur la fiche avec un message (attribut flash). L'auteur est l'utilisateur connecté.
 */
@Controller
class RecouvrementBackOfficeController {

    static final String CHEMIN = "/bo/recouvrement";
    static final String CHEMIN_FICHE = CHEMIN + "/dossiers/{id}";

    private static final Logger LOG = LoggerFactory.getLogger(RecouvrementBackOfficeController.class);

    private final SuiviRecouvrementService suivi;
    private final SuiviRemboursementService remboursements;
    private final Clock clock;

    RecouvrementBackOfficeController(SuiviRecouvrementService suivi, SuiviRemboursementService remboursements,
            Clock clock) {
        this.suivi = suivi;
        this.remboursements = remboursements;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String tableau(Model model) {
        TableauRecouvrement t = suivi.tableau();
        model.addAttribute("aVenir", t.aVenir().stream().map(VuePrelevement::depuis).toList());
        model.addAttribute("reussis", t.reussis().stream().map(VuePrelevement::depuis).toList());
        model.addAttribute("enEchec", t.enEchec().stream().map(VuePrelevement::depuis).toList());
        model.addAttribute("aVenirJusquAu", VuePrelevement.DATE.format(t.aVenirJusquAu()));
        model.addAttribute("reussisDepuis", VuePrelevement.DATE.format(t.reussisDepuis()));
        return "bo/recouvrement/tableau";
    }

    @GetMapping(CHEMIN_FICHE)
    String fiche(@PathVariable("id") UUID id, Model model) {
        try {
            model.addAttribute("fiche", VueFicheRecouvrement.depuis(id, suivi.fiche(id), clock));
        } catch (DossierIntrouvableException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("longueurNoteMax", NoteRecouvrement.LONGUEUR_MAX);
        model.addAttribute("longueurMotifMax", Demande.LONGUEUR_MOTIF_MAX);
        return "bo/recouvrement/fiche";
    }

    @PostMapping(CHEMIN_FICHE + "/notes")
    String noter(@PathVariable("id") UUID id, @RequestParam(name = "texte", required = false) String texte,
            Principal auteur, RedirectAttributes redirection) {
        return agir(id, redirection, "bo.recouvrement.note.ajoutee", () -> suivi.noter(id, auteur.getName(), texte));
    }

    @PostMapping(CHEMIN_FICHE + "/regularisation")
    String regulariser(@PathVariable("id") UUID id, Principal auteur, RedirectAttributes redirection) {
        return agir(id, redirection, "bo.recouvrement.regularise",
                () -> remboursements.regulariser(id, auteur.getName()));
    }

    @PostMapping(CHEMIN_FICHE + "/recouvrement")
    String passerEnRecouvrement(@PathVariable("id") UUID id,
            @RequestParam(name = "motif", required = false) String motif, Principal auteur,
            RedirectAttributes redirection) {
        return agir(id, redirection, "bo.recouvrement.en-recouvrement",
                () -> remboursements.passerEnRecouvrement(id, auteur.getName(), motif));
    }

    @PostMapping(CHEMIN_FICHE + "/solde")
    String solder(@PathVariable("id") UUID id, Principal auteur, RedirectAttributes redirection) {
        return agir(id, redirection, "bo.recouvrement.solde", () -> remboursements.solder(id, auteur.getName()));
    }

    static String fiche(UUID id) {
        return UriComponentsBuilder.fromPath(CHEMIN_FICHE).buildAndExpand(id).toUriString();
    }

    private static String agir(UUID id, RedirectAttributes redirection, String succes, Supplier<?> action) {
        try {
            action.get();
            redirection.addFlashAttribute("succes", succes);
        } catch (DossierIntrouvableException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (ErreurMetier e) {
            LOG.debug("Action de recouvrement refusée : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
        }
        return "redirect:" + fiche(id);
    }
}
