package com.seheli.identite.adapter.in.web;

import java.time.Clock;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.seheli.identite.application.DocumentService;

/** Back-office : derniers justificatifs reçus et leur consultation (rôles ANALYSTE et ADMIN). */
@Controller
class JustificatifsBackOfficeController {

    static final String CHEMIN = "/bo/justificatifs";
    static final int LIMITE = 100;

    private final DocumentService documents;
    private final Clock clock;

    JustificatifsBackOfficeController(DocumentService documents, Clock clock) {
        this.documents = documents;
        this.clock = clock;
    }

    record LigneBackOffice(LigneDocument document, String client) {
    }

    @GetMapping(CHEMIN)
    String liste(Model model) {
        model.addAttribute("documents", documents.derniersRecus(LIMITE).stream()
                .map(r -> new LigneBackOffice(LigneDocument.depuis(r.document(), clock),
                        (r.prenomClient() + " " + r.nomClient()).strip()))
                .toList());
        model.addAttribute("limite", LIMITE);
        return "bo/justificatifs/liste";
    }

    @GetMapping(CHEMIN + "/{id}")
    ResponseEntity<byte[]> voir(@PathVariable("id") UUID id) {
        return documents.contenuPourBackOffice(id)
                .map(ReponsesDocument::servir)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
