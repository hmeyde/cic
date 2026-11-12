package com.cic.demande.adapter.in.web;

import java.time.Clock;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.cic.demande.application.FicheDossierService;

/**
 * Back-office : fiche d'un dossier (client, emploi, justificatifs, score détaillé critère par
 * critère), rôle ANALYSTE (voir {@code SecurityConfig}). L'URL ne porte que l'identifiant technique
 * de la demande, aucune donnée personnelle.
 */
@Controller
class FicheDossierBackOfficeController {

    static final String CHEMIN = FileAnalyseBackOfficeController.CHEMIN + "/{id}";
    static final String VUE = "bo/dossiers/fiche";

    private final FicheDossierService fiches;
    private final Clock clock;

    FicheDossierBackOfficeController(FicheDossierService fiches, Clock clock) {
        this.fiches = fiches;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String fiche(@PathVariable("id") UUID id, Model model) {
        VueFicheDossier fiche = fiches.fiche(id).map(f -> VueFicheDossier.depuis(f, clock))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("fiche", fiche);
        return VUE;
    }
}
