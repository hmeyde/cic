package com.seheli.demande.adapter.in.web;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.seheli.commun.domain.ErreurMetier;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.AutorisationPdfService;
import com.seheli.contrat.application.ContratPdf;
import com.seheli.contrat.application.ContratPdfService;
import com.seheli.contrat.application.ContratService;
import com.seheli.demande.application.ApportInvalideException;
import com.seheli.demande.application.DemandeService;
import com.seheli.demande.application.PrealablesCommandeService;
import com.seheli.demande.application.RemiseProduitService;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.paiement.application.InstructionsApportService;

/**
 * Demandes du client connecté : soumission depuis la page d'un produit, liste de ses demandes et
 * suivi de son dossier en cours.
 * Le client est celui de la session (principal = identifiant du client), jamais un identifiant passé
 * dans la requête. Une règle de gestion non respectée ramène sur la page du produit avec son message.
 */
@Controller
class DemandeClientController {

    static final String CHEMIN = "/client/demandes";
    static final String VUE_LISTE = "client/demandes/liste";
    static final String CHEMIN_SUIVI = "/client/dossier";
    static final String VUE_SUIVI = "client/dossier/suivi";
    static final String CHEMIN_REMBOURSEMENT = "/client/dossier/remboursement";
    static final String VUE_REMBOURSEMENT = "client/dossier/remboursement";
    static final String CHEMIN_CONTRAT_PDF = "/client/dossier/contrat.pdf";
    static final String CHEMIN_AUTORISATION_PDF = "/client/dossier/autorisation-prelevement.pdf";
    static final String CHEMIN_REMISE = "/client/dossier/remise";
    static final String CHEMIN_PRODUIT = "/client/catalogue/{id}";

    private static final Logger LOG = LoggerFactory.getLogger(DemandeClientController.class);

    private final DemandeService demandes;
    private final ContratService contrats;
    private final ContratPdfService contratsPdf;
    private final AutorisationPdfService autorisationsPdf;
    private final InstructionsApportService instructionsApport;
    private final RemiseProduitService remises;
    private final PrealablesCommandeService prealables;
    private final Clock clock;
    private final ZoneId fuseau;

    DemandeClientController(DemandeService demandes, ContratService contrats, ContratPdfService contratsPdf,
            AutorisationPdfService autorisationsPdf, InstructionsApportService instructionsApport,
            RemiseProduitService remises, PrealablesCommandeService prealables, Clock clock) {
        this.demandes = demandes;
        this.contrats = contrats;
        this.contratsPdf = contratsPdf;
        this.autorisationsPdf = autorisationsPdf;
        this.instructionsApport = instructionsApport;
        this.remises = remises;
        this.prealables = prealables;
        this.clock = clock;
        this.fuseau = clock.getZone();
    }

    @GetMapping(CHEMIN)
    String liste(Principal principal, Model model) {
        model.addAttribute("demandes", demandes.parClient(clientId(principal)).stream()
                .map(d -> LigneDemande.depuis(d, fuseau)).toList());
        return VUE_LISTE;
    }

    /**
     * Suivi du dossier en cours : frise des huit jalons et carte « Prochaine action » ; une fois le
     * dossier accepté, l'offre (prix fixé, apport) et l'échéancier du contrat. Une offre annulée faute
     * d'apport n'est plus montrée. Tant que le dossier attend son apport (ACCEPTEE), les instructions de
     * paiement Masrvi (montant, code marchand, référence du dossier), le compte à rebours du délai et
     * l'état de l'apport et de l'autorisation. Le compte à rebours est calculé sur l'horloge du serveur
     * (celle de la démonstration), pas sur celle du navigateur. Aucun identifiant dans l'URL : le
     * dossier est celui du client connecté.
     */
    @GetMapping(CHEMIN_SUIVI)
    String suivi(Principal principal, Model model) {
        demandes.suiviDossierCourant(clientId(principal)).ifPresent(s -> {
            model.addAttribute("suivi", s);
            model.addAttribute("demande", LigneDemande.depuis(s.demande(), fuseau));
            ProchaineActionClient action = contrats.parDemande(s.demande().id()).map(c -> {
                if (s.statut() != StatutDossier.ANNULEE) {
                    model.addAttribute("offre", OffreClient.depuis(c, fuseau));
                }
                var instructions = instructionsApport.pour(c.numero(), c.apport());
                if (s.statut() == StatutDossier.ACCEPTEE) {
                    model.addAttribute("paiementApport", instructions);
                }
                return ProchaineActionClient.depuis(s, c, instructions, prealables.pour(s.demande().id()),
                        contrats.autorisation(c.id()).map(a -> a.statut()).orElse(null), LocalDate.now(clock));
            }).orElseGet(ProchaineActionClient::sansContrat);
            model.addAttribute("action", action);
        });
        return VUE_SUIVI;
    }

    /**
     * Remboursement du dossier en cours : anneau de progression (échéances payées sur le total), montant
     * restant, prochaine échéance avec sa date, liste des échéances avec leur statut et, en cas de retard,
     * la référence Masrvi pour régulariser, sans pénalité. Pas de {@code remboursement} dans le modèle tant
     * que le produit n'est pas remis. Aucun identifiant dans l'URL : le dossier est celui du client connecté.
     */
    @GetMapping(CHEMIN_REMBOURSEMENT)
    String remboursement(Principal principal, Model model) {
        demandes.suiviDossierCourant(clientId(principal)).ifPresent(s -> {
            model.addAttribute("suivi", s);
            if (RemboursementClient.concerne(s.statut())) {
                contrats.parDemande(s.demande().id()).ifPresent(c -> model.addAttribute("remboursement",
                        RemboursementClient.depuis(s.statut(), c,
                                instructionsApport.pour(c.numero(), c.apport()).codeMarchand(),
                                LocalDate.now(clock))));
            }
        });
        return VUE_REMBOURSEMENT;
    }

    /**
     * Contrat de vente à tempérament du dossier en cours, en PDF. Introuvable (404) tant que le dossier
     * n'est pas accepté, ou si l'offre est annulée. Aucun identifiant dans l'URL : le dossier est celui
     * du client connecté. Le document n'est pas mis en cache (données personnelles).
     */
    @GetMapping(CHEMIN_CONTRAT_PDF)
    ResponseEntity<byte[]> contratPdf(Principal principal) {
        return telechargement(document(principal, contratsPdf::pourDemande));
    }

    /**
     * Autorisation de prélèvement du dossier en cours (ordre de virement permanent vers le compte de
     * Seheli), remplie et à signer pour la banque du client. Mêmes règles que le contrat : 404 tant que le
     * dossier n'est pas accepté ou si l'offre est annulée, pas d'identifiant dans l'URL, pas de cache.
     */
    @GetMapping(CHEMIN_AUTORISATION_PDF)
    ResponseEntity<byte[]> autorisationPdf(Principal principal) {
        return telechargement(document(principal, autorisationsPdf::pourDemande));
    }

    private Optional<ContratPdf> document(Principal principal, Function<UUID, Optional<ContratPdf>> imprimer) {
        return demandes.suiviDossierCourant(clientId(principal))
                .filter(s -> s.statut() != StatutDossier.ANNULEE)
                .flatMap(s -> imprimer.apply(s.demande().id()));
    }

    private static ResponseEntity<byte[]> telechargement(Optional<ContratPdf> pdf) {
        return pdf.map(p -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(p.nomFichier()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(p.contenu()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Le client confirme avoir reçu son produit (dossier COMMANDEE) : le dossier passe à LIVREE puis
     * EN_REMBOURSEMENT. La case de confirmation doit être cochée. Aucun identifiant dans la requête :
     * le dossier est celui du client connecté. Retour au suivi avec le message de succès ou d'erreur.
     */
    @PostMapping(CHEMIN_REMISE)
    String confirmerRemise(Principal principal,
            @RequestParam(name = "confirmation", required = false) String confirmation,
            RedirectAttributes redirection) {
        try {
            remises.confirmer(clientId(principal), confirmation != null && !confirmation.isBlank());
            redirection.addFlashAttribute("succes", "client.remise.confirmee");
        } catch (ErreurMetier e) {
            LOG.debug("Remise non confirmée : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
        }
        return "redirect:" + CHEMIN_SUIVI;
    }

    /**
     * @param apport apport choisi en ouguiyas entières ; vide pour l'apport minimum
     */
    @PostMapping(CHEMIN)
    String soumettre(Principal principal, @RequestParam("produit") UUID produitId, @RequestParam("duree") int duree,
            @RequestParam(name = "apport", required = false) String apport, RedirectAttributes redirection) {
        try {
            demandes.soumettre(clientId(principal), produitId, duree, lireApport(apport));
            redirection.addFlashAttribute("succes", "client.demande.soumise");
            return "redirect:" + CHEMIN_SUIVI;
        } catch (ErreurMetier e) {
            LOG.debug("Demande non soumise : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
            return "redirect:" + UriComponentsBuilder.fromPath(CHEMIN_PRODUIT).queryParam("duree", duree)
                    .buildAndExpand(produitId).toUriString();
        }
    }

    /** Un apport illisible (lettres, décimales, négatif) est refusé comme un apport hors bornes. */
    static Optional<Montant> lireApport(String saisie) {
        if (saisie == null || saisie.isBlank()) {
            return Optional.empty();
        }
        String chiffres = saisie.replace(" ", "").replace(" ", "").replace(" ", "");
        if (!chiffres.matches("\\d{1,12}")) {
            throw new ApportInvalideException();
        }
        return Optional.of(Montant.de(Long.parseLong(chiffres)));
    }

    private static UUID clientId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
