package com.cic.demande.adapter.in.web;

import java.security.Principal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.cic.commun.domain.ErreurMetier;
import com.cic.commun.domain.Montant;
import com.cic.demande.application.ApportInvalideException;
import com.cic.demande.application.DemandeService;

/**
 * Demandes du client connecté : soumission depuis la page d'un produit, liste de ses demandes et
 * suivi de son dossier en cours.
 * Le client est celui de la session (principal = identifiant du client), jamais un identifiant passé
 * dans la requête. Une règle de gestion non respectée ramène sur la page du produit avec son message.
 */
@Controller
class DemandeClientController {

    static final String CHEMIN = "/client/demandes";
    static final String VUE_LISTE = "client/demandes/liste";
    static final String CHEMIN_SUIVI = "/client/dossier";
    static final String VUE_SUIVI = "client/dossier/suivi";
    static final String CHEMIN_PRODUIT = "/client/catalogue/{id}";

    private static final Logger LOG = LoggerFactory.getLogger(DemandeClientController.class);

    private final DemandeService demandes;
    private final ZoneId fuseau;

    DemandeClientController(DemandeService demandes, Clock clock) {
        this.demandes = demandes;
        this.fuseau = clock.getZone();
    }

    @GetMapping(CHEMIN)
    String liste(Principal principal, Model model) {
        model.addAttribute("demandes", demandes.parClient(clientId(principal)).stream()
                .map(d -> LigneDemande.depuis(d, fuseau)).toList());
        return VUE_LISTE;
    }

    /**
     * Suivi du dossier en cours : statut, étapes du parcours et prochaine étape. Aucun identifiant
     * dans l'URL : le dossier est celui du client connecté.
     */
    @GetMapping(CHEMIN_SUIVI)
    String suivi(Principal principal, Model model) {
        demandes.suiviDossierCourant(clientId(principal)).ifPresent(s -> {
            model.addAttribute("suivi", s);
            model.addAttribute("demande", LigneDemande.depuis(s.demande(), fuseau));
        });
        return VUE_SUIVI;
    }

    /**
     * @param apport apport choisi en ouguiyas entières ; vide pour l'apport minimum
     */
    @PostMapping(CHEMIN)
    String soumettre(Principal principal, @RequestParam("produit") UUID produitId, @RequestParam("duree") int duree,
            @RequestParam(name = "apport", required = false) String apport, RedirectAttributes redirection) {
        try {
            demandes.soumettre(clientId(principal), produitId, duree, lireApport(apport));
            redirection.addFlashAttribute("succes", "client.demande.soumise");
            return "redirect:" + CHEMIN_SUIVI;
        } catch (ErreurMetier e) {
            LOG.debug("Demande non soumise : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
            return "redirect:" + UriComponentsBuilder.fromPath(CHEMIN_PRODUIT).queryParam("duree", duree)
                    .buildAndExpand(produitId).toUriString();
        }
    }

    /** Un apport illisible (lettres, décimales, négatif) est refusé comme un apport hors bornes. */
    static Optional<Montant> lireApport(String saisie) {
        if (saisie == null || saisie.isBlank()) {
            return Optional.empty();
        }
        String chiffres = saisie.replace(" ", "").replace(" ", "").replace(" ", "");
        if (!chiffres.matches("\\d{1,12}")) {
            throw new ApportInvalideException();
        }
        return Optional.of(Montant.de(Long.parseLong(chiffres)));
    }

    private static UUID clientId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
