package com.cic.catalogue.adapter.in.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cic.catalogue.application.FournisseurInactifException;
import com.cic.catalogue.application.FournisseurIntrouvableException;
import com.cic.catalogue.application.FournisseurService;
import com.cic.catalogue.application.ProduitIntrouvableException;
import com.cic.catalogue.application.ProduitService;
import com.cic.catalogue.application.ReferenceProduitDejaUtiliseeException;
import com.cic.catalogue.domain.CategorieProduit;
import com.cic.catalogue.domain.Fournisseur;
import com.cic.catalogue.domain.Produit;

/**
 * Back-office : liste, création, modification et mise en indisponibilité des produits.
 * Réservé au rôle ADMIN (règle d'accès dans {@code SecurityConfig}).
 */
@Controller
@RequestMapping("/bo/produits")
class ProduitBackOfficeController {

    static final String VUE_LISTE = "bo/produits/liste";
    static final String VUE_FORMULAIRE = "bo/produits/formulaire";
    static final String REDIRECTION_LISTE = "redirect:/bo/produits";

    private final ProduitService produits;
    private final FournisseurService fournisseurs;

    ProduitBackOfficeController(ProduitService produits, FournisseurService fournisseurs) {
        this.produits = produits;
        this.fournisseurs = fournisseurs;
    }

    @GetMapping
    String liste(Model model) {
        Map<UUID, String> nomsFournisseurs = fournisseurs.lister().stream()
                .collect(Collectors.toMap(Fournisseur::id, Fournisseur::nom));
        model.addAttribute("produits", produits.lister());
        model.addAttribute("nomsFournisseurs", nomsFournisseurs);
        return VUE_LISTE;
    }

    @GetMapping("/nouveau")
    String nouveau(Model model) {
        return formulaire(model, new ProduitFormulaire(), null);
    }

    @PostMapping
    String creer(@ModelAttribute("formulaire") ProduitFormulaire formulaire, BindingResult erreurs,
                 Model model, RedirectAttributes redirection) {
        formulaire.valider(erreurs, true);
        if (!erreurs.hasErrors()) {
            try {
                produits.creer(formulaire.getReference(), formulaire.getDesignation(), formulaire.getMarque(),
                        formulaire.categorieChoisie().orElseThrow(), formulaire.fournisseurChoisi().orElseThrow(),
                        formulaire.prixSaisi().orElseThrow());
                redirection.addFlashAttribute("succes", "bo.produit.cree");
                return REDIRECTION_LISTE;
            } catch (ReferenceProduitDejaUtiliseeException e) {
                erreurs.rejectValue("reference", e.code());
            } catch (FournisseurInactifException | FournisseurIntrouvableException e) {
                erreurs.rejectValue("fournisseurId", e.code());
            }
        }
        return formulaire(model, formulaire, null);
    }

    @GetMapping("/{id}")
    String modification(@PathVariable UUID id, Model model) {
        Produit produit = trouver(id);
        return formulaire(model, ProduitFormulaire.de(produit), produit);
    }

    @PostMapping("/{id}")
    String modifier(@PathVariable UUID id, @ModelAttribute("formulaire") ProduitFormulaire formulaire,
                    BindingResult erreurs, Model model, RedirectAttributes redirection) {
        Produit produit = trouver(id);
        // La référence ne se modifie pas : on réaffiche toujours celle enregistrée.
        formulaire.setReference(produit.reference());
        formulaire.valider(erreurs, false);
        if (!erreurs.hasErrors()) {
            try {
                produits.modifier(produit.id(), formulaire.getDesignation(), formulaire.getMarque(),
                        formulaire.categorieChoisie().orElseThrow(), formulaire.fournisseurChoisi().orElseThrow(),
                        formulaire.prixSaisi().orElseThrow(), formulaire.isDisponible());
                redirection.addFlashAttribute("succes", "bo.produit.modifie");
                return REDIRECTION_LISTE;
            } catch (FournisseurInactifException | FournisseurIntrouvableException e) {
                erreurs.rejectValue("fournisseurId", e.code());
            }
        }
        return formulaire(model, formulaire, produit);
    }

    /** Action rapide depuis la liste : rendre un produit disponible ou indisponible. */
    @PostMapping("/{id}/disponibilite")
    String changerDisponibilite(@PathVariable UUID id, @RequestParam boolean disponible,
                                RedirectAttributes redirection) {
        try {
            produits.changerDisponibilite(id, disponible);
        } catch (ProduitIntrouvableException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirection.addFlashAttribute("succes", disponible ? "bo.produit.rendu.disponible" : "bo.produit.rendu.indisponible");
        return REDIRECTION_LISTE;
    }

    /**
     * Le choix du fournisseur propose les fournisseurs actifs, plus le fournisseur actuel du
     * produit s'il a été désactivé, pour ne pas perdre la valeur enregistrée.
     */
    private String formulaire(Model model, ProduitFormulaire formulaire, Produit produit) {
        UUID actuel = produit == null ? null : produit.fournisseurId();
        List<Fournisseur> choix = fournisseurs.lister().stream()
                .filter(f -> f.estActif() || f.id().equals(actuel))
                .toList();
        model.addAttribute("formulaire", formulaire);
        model.addAttribute("produitId", produit == null ? null : produit.id());
        model.addAttribute("categories", CategorieProduit.values());
        model.addAttribute("fournisseurs", choix);
        return VUE_FORMULAIRE;
    }

    private Produit trouver(UUID id) {
        try {
            return produits.parId(id);
        } catch (ProduitIntrouvableException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
