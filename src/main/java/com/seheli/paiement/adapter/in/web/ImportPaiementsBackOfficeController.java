package com.seheli.paiement.adapter.in.web;

import java.io.IOException;
import java.security.Principal;
import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.seheli.paiement.application.FichierCsvInvalideException;
import com.seheli.paiement.application.ImportPaiementsService;
import com.seheli.paiement.application.PaiementService;
import com.seheli.paiement.domain.ReferencePaiement;

/**
 * Back-office : import d'un fichier CSV de paiements reçus et liste des écarts (référence inconnue,
 * montant différent de l'apport attendu), rôles RECOUVREMENT et ADMIN comme la saisie (voir
 * {@code SecurityConfig}, {@code /bo/paiements/**}).
 *
 * <p>Après l'import, redirection vers la page d'import avec le bilan du fichier (attribut flash, en
 * session) : pas de second import au rechargement. Le fichier n'est pas conservé.
 */
@Controller
class ImportPaiementsBackOfficeController {

    static final String CHEMIN_IMPORT = "/bo/paiements/import";
    static final String CHEMIN_ECARTS = "/bo/paiements/ecarts";
    static final int LIMITE_ECARTS = 200;

    private static final Logger LOG = LoggerFactory.getLogger(ImportPaiementsBackOfficeController.class);

    private final ImportPaiementsService imports;
    private final PaiementService paiements;
    private final Clock clock;

    ImportPaiementsBackOfficeController(ImportPaiementsService imports, PaiementService paiements, Clock clock) {
        this.imports = imports;
        this.paiements = paiements;
        this.clock = clock;
    }

    @GetMapping(CHEMIN_IMPORT)
    String pageImport(Model model) {
        model.addAttribute("lignesMax", ImportPaiementsService.LIGNES_MAX);
        model.addAttribute("longueurReferenceMax", ReferencePaiement.LONGUEUR_MAX);
        return "bo/paiements/import";
    }

    @PostMapping(CHEMIN_IMPORT)
    String importer(@RequestParam(name = "fichier", required = false) MultipartFile fichier, Principal auteur,
            RedirectAttributes redirection) {
        try {
            if (fichier == null || fichier.isEmpty()) {
                throw new FichierCsvInvalideException(FichierCsvInvalideException.VIDE);
            }
            if (fichier.getSize() > ImportPaiementsService.TAILLE_MAX_OCTETS) {
                throw new FichierCsvInvalideException(FichierCsvInvalideException.TROP_GROS);
            }
            redirection.addFlashAttribute("resultat",
                    VueResultatImport.depuis(imports.importer(fichier.getBytes(), auteur.getName()), clock));
        } catch (FichierCsvInvalideException e) {
            LOG.debug("Fichier CSV de paiements refusé : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
        } catch (IOException e) {
            LOG.warn("Lecture du fichier CSV de paiements impossible", e);
            redirection.addFlashAttribute("erreur", FichierCsvInvalideException.VIDE);
        }
        return "redirect:" + CHEMIN_IMPORT;
    }

    @GetMapping(CHEMIN_ECARTS)
    String ecarts(Model model) {
        model.addAttribute("ecarts",
                paiements.ecarts(LIMITE_ECARTS).stream().map(p -> VuePaiement.depuis(p, clock)).toList());
        model.addAttribute("limite", LIMITE_ECARTS);
        return "bo/paiements/ecarts";
    }
}
