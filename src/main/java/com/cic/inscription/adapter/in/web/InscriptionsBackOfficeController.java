package com.cic.inscription.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.cic.demande.application.StatistiquesDemandeService;
import com.cic.inscription.application.InscriptionService;
import com.cic.inscription.domain.Inscription;

/**
 * Back-office : suivi de la pré-inscription publique ({@code /bo/inscriptions}). La page affiche le
 * nombre d'inscrits et le nombre de demandes complètes (demandes de crédit soumises par le parcours
 * client) ; l'export CSV ({@value #CHEMIN_EXPORT}) contient les données personnelles des inscrits et
 * n'est jamais mis en cache. Les droits d'accès sont posés dans {@code SecurityConfig}.
 */
@Controller
class InscriptionsBackOfficeController {

    static final String CHEMIN = "/bo/inscriptions";
    static final String CHEMIN_EXPORT = "/bo/inscriptions/export.csv";

    private static final Logger LOG = LoggerFactory.getLogger(InscriptionsBackOfficeController.class);
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final InscriptionService inscriptions;
    private final StatistiquesDemandeService demandes;
    private final Clock clock;

    InscriptionsBackOfficeController(InscriptionService inscriptions, StatistiquesDemandeService demandes,
            Clock clock) {
        this.inscriptions = inscriptions;
        this.demandes = demandes;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String tableau(Model model) {
        model.addAttribute("nombreInscrits", inscriptions.nombre());
        model.addAttribute("nombreDemandesCompletes", demandes.nombreDemandesCompletes());
        return "bo/inscriptions/tableau";
    }

    @GetMapping(CHEMIN_EXPORT)
    ResponseEntity<byte[]> export() {
        List<Inscription> toutes = inscriptions.toutes();
        byte[] csv = InscriptionsCsv.ecrire(toutes, clock.getZone());
        LOG.info("Export CSV des pré-inscriptions : {} lignes", toutes.size());
        return ResponseEntity.ok()
                .contentType(CSV)
                .contentLength(csv.length)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("inscriptions-" + LocalDate.now(clock) + ".csv").build().toString())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(csv);
    }
}
