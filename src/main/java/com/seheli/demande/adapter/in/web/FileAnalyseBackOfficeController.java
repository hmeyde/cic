package com.seheli.demande.adapter.in.web;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.seheli.demande.application.DemandeService;
import com.seheli.demande.application.DossierEnAnalyse;

/**
 * Back-office : file des dossiers EN_ANALYSE, du plus ancien au plus récent (rôle ANALYSTE, voir
 * {@code SecurityConfig}).
 */
@Controller
class FileAnalyseBackOfficeController {

    static final String CHEMIN = "/bo/dossiers";
    static final String VUE = "bo/dossiers/file";

    private final DemandeService demandes;
    private final Clock clock;

    FileAnalyseBackOfficeController(DemandeService demandes, Clock clock) {
        this.demandes = demandes;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String file(Model model) {
        List<DossierEnAnalyse> file = demandes.fileAnalyse();
        Instant maintenant = clock.instant();
        model.addAttribute("dossiers", IntStream.range(0, file.size())
                .mapToObj(i -> LigneFileAnalyse.depuis(i + 1, file.get(i), maintenant, clock.getZone()))
                .toList());
        return VUE;
    }
}
