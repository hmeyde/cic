package com.cic.catalogue.adapter.in.web;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.cic.catalogue.application.CatalogueClientService;
import com.cic.catalogue.application.ProduitPropose;
import com.cic.catalogue.domain.CategorieProduit;
import com.cic.contrat.domain.Simulation;

/**
 * Espace client : catalogue des produits proposés (filtrable par catégorie) et page d'un produit
 * avec la simulation HTMX (durée → prix total, apport, mensualité) calculée par le module contrat,
 * suivie du formulaire de demande (envoyé au module demande, {@code POST /client/demandes}).
 * Réservé au rôle CLIENT (chaîne de sécurité {@code /client/**}). Un produit non proposé répond 404.
 */
@Controller
class CatalogueClientController {

    static final String CHEMIN = "/client/catalogue";
    static final String VUE_LISTE = "client/catalogue/liste";
    static final String VUE_PRODUIT = "client/catalogue/produit";
    static final String FRAGMENT_SIMULATION = "client/catalogue/simulation :: resultat";

    private final CatalogueClientService catalogue;

    CatalogueClientController(CatalogueClientService catalogue) {
        this.catalogue = catalogue;
    }

    /** Une catégorie inconnue est ignorée : tout le catalogue est affiché. */
    @GetMapping(CHEMIN)
    String liste(@RequestParam(name = "categorie", required = false) String categorie, Model model) {
        Optional<CategorieProduit> choisie = categorieOuVide(categorie);
        model.addAttribute("produits", catalogue.proposes(choisie));
        model.addAttribute("categories", CategorieProduit.values());
        model.addAttribute("categorieChoisie", choisie.map(Enum::name).orElse(""));
        return VUE_LISTE;
    }

    /**
     * Page du produit avec la simulation sur la durée demandée ; une durée absente ou hors bornes
     * est remplacée par la durée maximale. Sans JavaScript, le formulaire de simulation renvoie ici.
     */
    @GetMapping(CHEMIN + "/{id}")
    String produit(@PathVariable("id") UUID id, @RequestParam(name = "duree", required = false) Integer duree,
            Model model) {
        ProduitPropose produit = catalogue.propose(id).orElseThrow(CatalogueClientController::introuvable);
        int retenue = duree != null && duree >= produit.dureeMinMois() && duree <= produit.dureeMaxMois()
                ? duree : produit.dureeMaxMois();
        model.addAttribute("produit", produit);
        model.addAttribute("produitId", id);
        model.addAttribute("durees", IntStream.rangeClosed(produit.dureeMinMois(), produit.dureeMaxMois())
                .boxed().toList());
        model.addAttribute("simulation",
                catalogue.simuler(id, retenue).orElseThrow(CatalogueClientController::introuvable));
        return VUE_PRODUIT;
    }

    /** Fragment HTMX : résultat de la simulation, recalculé à chaque changement de durée. */
    @GetMapping(CHEMIN + "/{id}/simulation")
    String simulation(@PathVariable("id") UUID id, @RequestParam("duree") int duree, Model model) {
        Simulation simulation;
        try {
            simulation = catalogue.simuler(id, duree).orElseThrow(CatalogueClientController::introuvable);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        model.addAttribute("simulation", simulation);
        model.addAttribute("produitId", id);
        return FRAGMENT_SIMULATION;
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
