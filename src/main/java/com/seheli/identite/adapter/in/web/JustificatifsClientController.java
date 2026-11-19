package com.seheli.identite.adapter.in.web;

import java.io.IOException;
import java.security.Principal;
import java.time.Clock;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.seheli.commun.domain.ErreurMetier;
import com.seheli.identite.application.DocumentService;
import com.seheli.identite.application.DocumentTropVolumineuxException;
import com.seheli.identite.application.DocumentVideException;
import com.seheli.identite.domain.Document;
import com.seheli.identite.domain.TypeDocument;

/**
 * Justificatifs du client connecté (étape 3 du parcours de demande) : envoi, liste et consultation.
 * Le type proposé par défaut est la prochaine pièce attendue. Un fichier peut venir du sélecteur
 * (ou d'un glisser-déposer) ou d'une photo prise directement (champ {@code photo}). Le client est celui de la session ;
 * un document d'un autre client répond 404, comme un document inexistant.
 */
@Controller
class JustificatifsClientController {

    static final String CHEMIN = "/client/justificatifs";
    static final String VUE = "client/justificatifs";

    private final DocumentService documents;
    private final Clock clock;

    JustificatifsClientController(DocumentService documents, Clock clock) {
        this.documents = documents;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String liste(Principal principal, Model model) {
        return vue(model, clientId(principal), null, null);
    }

    @PostMapping(CHEMIN)
    String envoyer(Principal principal, @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "fichier", required = false) MultipartFile fichier,
            @RequestParam(name = "photo", required = false) MultipartFile photo, Model model,
            RedirectAttributes redirection) throws IOException {
        UUID clientId = clientId(principal);
        fichier = fichierOuPhoto(fichier, photo);
        TypeDocument typeDocument = typeOuNull(type);
        if (typeDocument == null) {
            String vue = vue(model, clientId, null, "client.justificatifs.type.obligatoire");
            model.addAttribute("typeChoisi", "");
            return vue;
        }
        try {
            if (fichier == null || fichier.isEmpty()) {
                throw new DocumentVideException();
            }
            if (fichier.getSize() > Document.TAILLE_MAX_OCTETS) {
                throw new DocumentTropVolumineuxException();
            }
            documents.envoyer(clientId, typeDocument, fichier.getBytes());
            redirection.addFlashAttribute("succes", "client.justificatifs.envoye");
            return "redirect:" + CHEMIN;
        } catch (ErreurMetier e) {
            return vue(model, clientId, typeDocument, e.code());
        }
    }

    @GetMapping(CHEMIN + "/{id}")
    ResponseEntity<byte[]> voir(Principal principal, @PathVariable("id") UUID id) {
        return documents.contenuPourClient(clientId(principal), id)
                .map(ReponsesDocument::servir)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String vue(Model model, UUID clientId, TypeDocument typeChoisi, String erreur) {
        List<Document> recus = documents.parClient(clientId);
        Set<TypeDocument> typesRecus = recus.stream().map(Document::type)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TypeDocument.class)));
        model.addAttribute("documents", recus.stream().map(d -> LigneDocument.depuis(d, clock)).toList());
        model.addAttribute("types", TypeDocument.values());
        model.addAttribute("typesRecus", typesRecus);
        // Par défaut, la prochaine pièce attendue : une seule question à la fois.
        TypeDocument suivant = typeChoisi != null ? typeChoisi
                : Arrays.stream(TypeDocument.values()).filter(t -> !typesRecus.contains(t)).findFirst().orElse(null);
        model.addAttribute("typeChoisi", suivant == null ? "" : suivant.name());
        model.addAttribute("nombreRecus", typesRecus.size());
        model.addAttribute("complets", typesRecus.size() == TypeDocument.values().length);
        model.addAttribute("tailleMaxOctets", Document.TAILLE_MAX_OCTETS);
        model.addAttribute("tailleMaxMo", Document.TAILLE_MAX_MO);
        model.addAttribute("maxParType", DocumentService.MAX_PAR_TYPE);
        model.addAttribute("erreur", erreur);
        return VUE;
    }

    /**
     * Le fichier choisi, sinon la photo prise avec l'appareil (champ séparé, avec {@code capture}) ;
     * sans JavaScript les deux champs sont envoyés, l'un vide.
     */
    static MultipartFile fichierOuPhoto(MultipartFile fichier, MultipartFile photo) {
        return fichier != null && !fichier.isEmpty() ? fichier : photo != null && !photo.isEmpty() ? photo : fichier;
    }

    private static TypeDocument typeOuNull(String type) {
        if (type == null) {
            return null;
        }
        try {
            return TypeDocument.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UUID clientId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
