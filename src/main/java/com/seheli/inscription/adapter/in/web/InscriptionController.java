package com.seheli.inscription.adapter.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.seheli.inscription.application.InscriptionService;
import com.seheli.inscription.application.SaisieInscription;
import com.seheli.inscription.domain.Inscription;
import com.seheli.inscription.domain.InscriptionInvalideException;
import com.seheli.inscription.domain.TelephoneDejaInscritException;
import com.seheli.inscription.domain.TrancheSalaire;
import com.seheli.inscription.domain.TypeContrat;

/**
 * Page publique de pré-inscription ({@code /inscription}), en français et en arabe, protégée par CSRF.
 *
 * <p>Champ piège anti-robot : le champ {@value #CHAMP_PIEGE} est caché aux humains (hors écran, hors
 * tabulation, ignoré des lecteurs d'écran). S'il est rempli, l'envoi vient d'un robot : rien n'est
 * enregistré, mais la page de remerciement s'affiche comme d'habitude pour ne pas le renseigner.
 *
 * <p>Après un enregistrement, redirection vers la page de remerciement (pas de double envoi au
 * rechargement). Une saisie refusée réaffiche le formulaire avec les valeurs tapées et le message
 * à côté du champ fautif ; aucune donnée personnelle ne passe dans l'URL ni dans les journaux.
 */
@Controller
class InscriptionController {

    static final String CHEMIN = "/inscription";
    static final String CHEMIN_MERCI = "/inscription/merci";
    static final String CHAMP_PIEGE = "siteWeb";

    private static final Logger LOG = LoggerFactory.getLogger(InscriptionController.class);

    private final InscriptionService inscriptions;

    InscriptionController(InscriptionService inscriptions) {
        this.inscriptions = inscriptions;
    }

    @GetMapping(CHEMIN)
    String formulaire(Model model) {
        return afficher(model, SaisieInscription.vide());
    }

    @PostMapping(CHEMIN)
    String inscrire(@RequestParam(name = "nom", required = false) String nom,
            @RequestParam(name = "telephone", required = false) String telephone,
            @RequestParam(name = "employeur", required = false) String employeur,
            @RequestParam(name = "typeContrat", required = false) String typeContrat,
            @RequestParam(name = "trancheSalaire", required = false) String trancheSalaire,
            @RequestParam(name = "produitSouhaite", required = false) String produitSouhaite,
            @RequestParam(name = "consentement", required = false) String consentement,
            @RequestParam(name = CHAMP_PIEGE, required = false) String piege, Model model) {
        if (piege != null && !piege.isBlank()) {
            LOG.info("Pré-inscription ignorée : champ piège rempli");
            return "redirect:" + CHEMIN_MERCI;
        }
        SaisieInscription saisie = new SaisieInscription(nom, telephone, employeur, typeContrat, trancheSalaire,
                produitSouhaite, consentement != null);
        try {
            inscriptions.inscrire(saisie);
            return "redirect:" + CHEMIN_MERCI;
        } catch (InscriptionInvalideException e) {
            LOG.debug("Pré-inscription refusée : {}", e.code());
            model.addAttribute("erreurChamp", e.champ());
            model.addAttribute("erreur", e.code());
        } catch (TelephoneDejaInscritException e) {
            LOG.debug("Pré-inscription refusée : {}", e.code());
            model.addAttribute("erreurChamp", "telephone");
            model.addAttribute("erreur", e.code());
        }
        return afficher(model, saisie);
    }

    @GetMapping(CHEMIN_MERCI)
    String merci() {
        return "inscription/merci";
    }

    private static String afficher(Model model, SaisieInscription saisie) {
        model.addAttribute("saisie", saisie);
        model.addAttribute("typesContrat", TypeContrat.values());
        model.addAttribute("tranchesSalaire", TrancheSalaire.values());
        model.addAttribute("nomMax", Inscription.NOM_LONGUEUR_MAX);
        model.addAttribute("employeurMax", Inscription.EMPLOYEUR_LONGUEUR_MAX);
        model.addAttribute("produitMax", Inscription.PRODUIT_LONGUEUR_MAX);
        model.addAttribute("champPiege", CHAMP_PIEGE);
        return "inscription/formulaire";
    }
}
