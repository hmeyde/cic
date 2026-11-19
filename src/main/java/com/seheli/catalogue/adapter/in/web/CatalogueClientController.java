package com.seheli.catalogue.adapter.in.web;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.seheli.catalogue.application.CatalogueClientService;
import com.seheli.catalogue.application.ProduitPropose;
import com.seheli.catalogue.application.SimulationClient;
import com.seheli.catalogue.application.SimulationClientService;
import com.seheli.catalogue.domain.CategorieProduit;

/**
 * Espace client : catalogue des produits proposés (filtrable par catégorie) et page d'un produit
 * avec la simulation HTMX (durée et apport → échéancier daté, décomposition du prix, alerte
 * d'endettement) calculée par le module contrat,
 * suivie du formulaire de demande (envoyé au module demande, {@code POST /client/demandes}).
 * Réservé au rôle CLIENT (chaîne de sécurité {@code /client/**}). Un produit non proposé répond 404.
 */
@Controller
class CatalogueClientController {

    static final String CHEMIN = "/client/catalogue";
    static final String VUE_LISTE = "client/catalogue/liste";
    static final String VUE_PRODUIT = "client/catalogue/produit";
    static final String FRAGMENT_RESULTATS = VUE_LISTE + " :: resultats";
    static final int RECHERCHE_LONGUEUR_MAX = 100;
    static final String FRAGMENT_SIMULATION = "client/catalogue/simulation :: reponse";

    private final CatalogueClientService catalogue;
    private final SimulationClientService simulations;

    CatalogueClientController(CatalogueClientService catalogue, SimulationClientService simulations) {
        this.catalogue = catalogue;
        this.simulations = simulations;
    }

    /**
     * Catalogue filtré par catégorie (puces) et par recherche libre. Une catégorie inconnue est
     * ignorée ; la recherche est tronquée à {@value #RECHERCHE_LONGUEUR_MAX} caractères.
     * Une requête HTMX (recherche au fil de la frappe, clic sur une puce) ne reçoit que la zone des
     * résultats ; la restauration d'historique HTMX et la navigation classique reçoivent la page.
     */
    @GetMapping(CHEMIN)
    String liste(@RequestParam(name = "categorie", required = false) String categorie,
            @RequestParam(name = "q", required = false) String q,
            @RequestHeader(name = "HX-Request", required = false) String htmx,
            @RequestHeader(name = "HX-History-Restore-Request", required = false) String restauration,
            Model model, HttpServletResponse response) {
        Optional<CategorieProduit> choisie = categorieOuVide(categorie);
        String recherche = q == null ? "" : q.strip();
        if (recherche.length() > RECHERCHE_LONGUEUR_MAX) {
            recherche = recherche.substring(0, RECHERCHE_LONGUEUR_MAX);
        }
        List<ProduitPropose> produits = catalogue.proposes(choisie, recherche);
        model.addAttribute("produits", produits);
        model.addAttribute("illustrations", produits.stream()
                .collect(Collectors.toMap(ProduitPropose::id, IllustrationProduit::pour)));
        model.addAttribute("categories", CategorieProduit.values());
        model.addAttribute("categorieChoisie", choisie.map(Enum::name).orElse(""));
        model.addAttribute("recherche", recherche);
        response.setHeader(HttpHeaders.VARY, "HX-Request");
        return "true".equals(htmx) && !"true".equals(restauration) ? FRAGMENT_RESULTATS : VUE_LISTE;
    }

    /**
     * Page du produit avec la simulation sur la durée et l'apport demandés ; une durée absente ou
     * hors bornes est remplacée par la durée maximale, un apport hors bornes est ramené dans les
     * bornes du curseur. Sans JavaScript, le formulaire de simulation renvoie ici.
     */
    @GetMapping(CHEMIN + "/{id}")
    String produit(@PathVariable("id") UUID id, @RequestParam(name = "duree", required = false) Integer duree,
            @RequestParam(name = "apport", required = false) Integer apport, Principal principal, Model model) {
        ProduitPropose produit = catalogue.propose(id).orElseThrow(CatalogueClientController::introuvable);
        int retenue = duree != null && duree >= produit.dureeMinMois() && duree <= produit.dureeMaxMois()
                ? duree : produit.dureeMaxMois();
        model.addAttribute("produit", produit);
        model.addAttribute("illustration", IllustrationProduit.pour(produit));
        model.addAttribute("produitId", id);
        model.addAttribute("durees", IntStream.rangeClosed(produit.dureeMinMois(), produit.dureeMaxMois())
                .boxed().toList());
        model.addAttribute("simulation", simulations.simuler(id, clientId(principal), retenue, apport)
                .orElseThrow(CatalogueClientController::introuvable));
        return VUE_PRODUIT;
    }

    /**
     * Fragment HTMX : frise de l'échéancier et décomposition du prix, recalculées à chaque
     * changement de durée ou d'apport. Une durée hors bornes répond 400.
     */
    @GetMapping(CHEMIN + "/{id}/simulation")
    String simulation(@PathVariable("id") UUID id, @RequestParam("duree") int duree,
            @RequestParam(name = "apport", required = false) Integer apport, Principal principal, Model model) {
        SimulationClient simulation;
        try {
            simulation = simulations.simuler(id, clientId(principal), duree, apport)
                    .orElseThrow(CatalogueClientController::introuvable);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        model.addAttribute("simulation", simulation);
        model.addAttribute("produitId", id);
        return FRAGMENT_SIMULATION;
    }

    private static UUID clientId(Principal principal) {
        return UUID.fromString(principal.getName());
    }

    private static ResponseStatusException introuvable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private static Optional<CategorieProduit> categorieOuVide(String valeur) {
        if (valeur == null) {
            return Optional.empty();
        }
        return Arrays.stream(CategorieProduit.values()).filter(c -> c.name().equals(valeur)).findFirst();
    }
}
