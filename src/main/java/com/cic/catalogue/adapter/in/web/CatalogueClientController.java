package com.cic.catalogue.adapter.in.web;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.cic.catalogue.application.CatalogueClientService;
import com.cic.catalogue.domain.CategorieProduit;

/**
 * Espace client : catalogue des produits proposés (filtrable par catégorie) et page d'un produit.
 * Réservé au rôle CLIENT (chaîne de sécurité {@code /client/**}). Un produit non proposé répond 404.
 */
@Controller
class CatalogueClientController {

    static final String CHEMIN = "/client/catalogue";
    static final String VUE_LISTE = "client/catalogue/liste";
    static final String VUE_PRODUIT = "client/catalogue/produit";

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

    @GetMapping(CHEMIN + "/{id}")
    String produit(@PathVariable("id") UUID id, Model model) {
        model.addAttribute("produit", catalogue.propose(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return VUE_PRODUIT;
    }

    private static Optional<CategorieProduit> categorieOuVide(String valeur) {
        if (valeur == null) {
            return Optional.empty();
        }
        return Arrays.stream(CategorieProduit.values()).filter(c -> c.name().equals(valeur)).findFirst();
    }
}
