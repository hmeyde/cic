package com.cic.catalogue.adapter.in.web;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cic.catalogue.application.BornesRemise;
import com.cic.catalogue.application.FournisseurIntrouvableException;
import com.cic.catalogue.application.FournisseurService;
import com.cic.catalogue.application.NomFournisseurDejaUtiliseException;
import com.cic.catalogue.application.RemiseHorsBornesException;
import com.cic.catalogue.domain.Fournisseur;

/**
 * Back-office : liste, création et modification des fournisseurs. Réservé au rôle ADMIN
 * (règle d'accès dans {@code SecurityConfig}).
 */
@Controller
@RequestMapping("/bo/fournisseurs")
class FournisseurBackOfficeController {

    static final String VUE_LISTE = "bo/fournisseurs/liste";
    static final String VUE_FORMULAIRE = "bo/fournisseurs/formulaire";
    static final String REDIRECTION_LISTE = "redirect:/bo/fournisseurs";

    private final FournisseurService service;

    FournisseurBackOfficeController(FournisseurService service) {
        this.service = service;
    }

    @GetMapping
    String liste(Model model) {
        model.addAttribute("fournisseurs", service.lister());
        return VUE_LISTE;
    }

    @GetMapping("/nouveau")
    String nouveau(Model model) {
        return formulaire(model, new FournisseurFormulaire(), null);
    }

    @PostMapping
    String creer(@ModelAttribute("formulaire") FournisseurFormulaire formulaire, BindingResult erreurs,
                 Model model, RedirectAttributes redirection) {
        formulaire.valider(erreurs);
        if (!erreurs.hasErrors()) {
            try {
                service.creer(formulaire.getNom(), formulaire.remisePct().orElseThrow());
                redirection.addFlashAttribute("succes", "bo.fournisseur.cree");
                return REDIRECTION_LISTE;
            } catch (RemiseHorsBornesException e) {
                rejeterRemise(e, erreurs);
            } catch (NomFournisseurDejaUtiliseException e) {
                erreurs.rejectValue("nom", e.code());
            }
        }
        return formulaire(model, formulaire, null);
    }

    @GetMapping("/{id}")
    String modification(@PathVariable UUID id, Model model) {
        Fournisseur fournisseur = trouver(id);
        return formulaire(model, FournisseurFormulaire.de(fournisseur), fournisseur.id());
    }

    @PostMapping("/{id}")
    String modifier(@PathVariable UUID id, @ModelAttribute("formulaire") FournisseurFormulaire formulaire,
                    BindingResult erreurs, Model model, RedirectAttributes redirection) {
        UUID fournisseurId = trouver(id).id();
        formulaire.valider(erreurs);
        if (!erreurs.hasErrors()) {
            try {
                BigDecimal remise = formulaire.remisePct().orElseThrow();
                service.modifier(fournisseurId, formulaire.getNom(), remise, formulaire.isActif());
                redirection.addFlashAttribute("succes", "bo.fournisseur.modifie");
                return REDIRECTION_LISTE;
            } catch (RemiseHorsBornesException e) {
                rejeterRemise(e, erreurs);
            } catch (NomFournisseurDejaUtiliseException e) {
                erreurs.rejectValue("nom", e.code());
            }
        }
        return formulaire(model, formulaire, fournisseurId);
    }

    private String formulaire(Model model, FournisseurFormulaire formulaire, UUID id) {
        BornesRemise bornes = service.bornesRemise();
        model.addAttribute("formulaire", formulaire);
        model.addAttribute("fournisseurId", id);
        model.addAttribute("remiseMin", texte(bornes.minPct()));
        model.addAttribute("remiseMax", texte(bornes.maxPct()));
        return VUE_FORMULAIRE;
    }

    private Fournisseur trouver(UUID id) {
        try {
            return service.parId(id);
        } catch (FournisseurIntrouvableException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static void rejeterRemise(RemiseHorsBornesException e, BindingResult erreurs) {
        erreurs.rejectValue("remise", e.code(), new Object[] {texte(e.minPct()), texte(e.maxPct())}, null);
    }

    private static String texte(BigDecimal pct) {
        return pct.stripTrailingZeros().toPlainString();
    }
}
