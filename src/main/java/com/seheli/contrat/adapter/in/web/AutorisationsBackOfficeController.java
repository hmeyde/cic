package com.seheli.contrat.adapter.in.web;

import java.io.IOException;
import java.security.Principal;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.util.UriComponentsBuilder;

import com.seheli.commun.domain.ErreurMetier;
import com.seheli.contrat.application.AutorisationIntrouvableException;
import com.seheli.contrat.application.AutorisationPdfService;
import com.seheli.contrat.application.ContratPdf;
import com.seheli.contrat.application.DocumentSigneContenu;
import com.seheli.contrat.application.SaisieAutorisationInvalideException;
import com.seheli.contrat.application.SuiviAutorisationService;
import com.seheli.contrat.domain.DocumentSigne;
import com.seheli.contrat.domain.StatutAutorisation;

/**
 * Back-office : suivi des autorisations de prélèvement, rôle ANALYSTE (voir {@code SecurityConfig}).
 * Liste filtrable par statut ; fiche de chaque autorisation avec son type, son statut, le document
 * signé par le client (dépôt et consultation), l'autorisation à imprimer et les actions possibles
 * (transmise à la banque, acceptée, active, révoquée avec un motif). L'URL ne porte que des
 * identifiants techniques ; l'auteur est l'utilisateur connecté.
 */
@Controller
class AutorisationsBackOfficeController {

    static final String CHEMIN = "/bo/autorisations";
    static final String CHEMIN_FICHE = CHEMIN + "/{id}";
    static final int LIMITE = 100;

    private static final Logger LOG = LoggerFactory.getLogger(AutorisationsBackOfficeController.class);

    private final SuiviAutorisationService suivi;
    private final AutorisationPdfService autorisationsPdf;
    private final Clock clock;

    AutorisationsBackOfficeController(SuiviAutorisationService suivi, AutorisationPdfService autorisationsPdf,
            Clock clock) {
        this.suivi = suivi;
        this.autorisationsPdf = autorisationsPdf;
        this.clock = clock;
    }

    @GetMapping(CHEMIN)
    String liste(@RequestParam(name = "statut", required = false) String statut, Model model) {
        Optional<StatutAutorisation> filtre = statut(statut);
        model.addAttribute("autorisations", suivi.liste(filtre, LIMITE).stream()
                .map(l -> VueAutorisation.depuis(l, clock)).toList());
        model.addAttribute("statuts", StatutAutorisation.values());
        model.addAttribute("statutChoisi", filtre.map(Enum::name).orElse(""));
        model.addAttribute("limite", LIMITE);
        return "bo/autorisations/liste";
    }

    @GetMapping(CHEMIN_FICHE)
    String fiche(@PathVariable("id") UUID id, Model model) {
        VueAutorisation vue = suivi.fiche(id).map(f -> VueAutorisation.depuis(f, clock))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("autorisation", vue);
        model.addAttribute("tailleMaxMo", DocumentSigne.TAILLE_MAX_OCTETS / (1024 * 1024));
        model.addAttribute("longueurMotifMax", SuiviAutorisationService.LONGUEUR_MOTIF_MAX);
        return "bo/autorisations/fiche";
    }

    @PostMapping(CHEMIN_FICHE + "/document-signe")
    String deposer(@PathVariable("id") UUID id,
            @RequestParam(name = "fichier", required = false) MultipartFile fichier, Principal auteur,
            RedirectAttributes redirection) {
        return agir(id, redirection, "bo.autorisation.document.depose", () -> {
            if (fichier == null || fichier.isEmpty()) {
                throw new SaisieAutorisationInvalideException(SaisieAutorisationInvalideException.DOCUMENT_VIDE);
            }
            if (fichier.getSize() > DocumentSigne.TAILLE_MAX_OCTETS) {
                throw new SaisieAutorisationInvalideException(
                        SaisieAutorisationInvalideException.DOCUMENT_TROP_VOLUMINEUX);
            }
            try {
                return suivi.deposerDocumentSigne(id, auteur.getName(), fichier.getBytes());
            } catch (IOException e) {
                throw new IllegalStateException("Lecture du fichier envoyé impossible", e);
            }
        });
    }

    @GetMapping(CHEMIN_FICHE + "/document-signe")
    ResponseEntity<byte[]> documentSigne(@PathVariable("id") UUID id) {
        DocumentSigneContenu c = suivi.documentSigne(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(c.document().format().typeMime()))
                .contentLength(c.contenu().length)
                .header("Content-Disposition", ContentDisposition.inline()
                        .filename("autorisation-signee." + c.document().format().extension()).build().toString())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(c.contenu());
    }

    /** Autorisation à faire signer, imprimée au guichet si le client ne l'a pas téléchargée. */
    @GetMapping(CHEMIN_FICHE + "/autorisation.pdf")
    ResponseEntity<byte[]> aSigner(@PathVariable("id") UUID id) {
        UUID demandeId = suivi.fiche(id).map(f -> f.autorisation().demandeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ContratPdf pdf = autorisationsPdf.pourDemande(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.contenu().length)
                .header("Content-Disposition", ContentDisposition.inline().filename(pdf.nomFichier()).build()
                        .toString())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(pdf.contenu());
    }

    @PostMapping(CHEMIN_FICHE + "/transmission")
    String transmettre(@PathVariable("id") UUID id, Principal auteur, RedirectAttributes redirection) {
        return agir(id, redirection, "bo.autorisation.transmise", () -> suivi.transmettre(id, auteur.getName()));
    }

    @PostMapping(CHEMIN_FICHE + "/acceptation")
    String accepter(@PathVariable("id") UUID id, Principal auteur, RedirectAttributes redirection) {
        return agir(id, redirection, "bo.autorisation.acceptee", () -> suivi.accepter(id, auteur.getName()));
    }

    @PostMapping(CHEMIN_FICHE + "/activation")
    String activer(@PathVariable("id") UUID id, Principal auteur, RedirectAttributes redirection) {
        return agir(id, redirection, "bo.autorisation.active", () -> suivi.activer(id, auteur.getName()));
    }

    @PostMapping(CHEMIN_FICHE + "/revocation")
    String revoquer(@PathVariable("id") UUID id, @RequestParam(name = "motif", required = false) String motif,
            Principal auteur, RedirectAttributes redirection) {
        return agir(id, redirection, "bo.autorisation.revoquee", () -> suivi.revoquer(id, auteur.getName(), motif));
    }

    private static String agir(UUID id, RedirectAttributes redirection, String succes, Supplier<?> action) {
        try {
            action.get();
            redirection.addFlashAttribute("succes", succes);
        } catch (AutorisationIntrouvableException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (ErreurMetier e) {
            LOG.debug("Suivi de l'autorisation non enregistré : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
        }
        return "redirect:" + UriComponentsBuilder.fromPath(CHEMIN_FICHE).buildAndExpand(id).toUriString();
    }

    private static Optional<StatutAutorisation> statut(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(StatutAutorisation.valueOf(valeur));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
