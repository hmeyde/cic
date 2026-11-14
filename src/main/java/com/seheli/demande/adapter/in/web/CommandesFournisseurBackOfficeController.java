package com.seheli.demande.adapter.in.web;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.seheli.commun.domain.ErreurMetier;
import com.seheli.demande.application.BonCommandePdf;
import com.seheli.demande.application.CommandeFournisseurService;
import com.seheli.demande.application.DossierACommander;
import com.seheli.demande.application.DossierIntrouvableException;
import com.seheli.demande.application.LigneCommande;
import com.seheli.demande.domain.CommandeFournisseur;
import com.seheli.demande.domain.Demande;

/**
 * Back-office : commandes fournisseur, rôles ANALYSTE et ADMIN (voir {@code SecurityConfig}). Liste
 * des dossiers APPORT_ET_AUTORISATION avec leur bon de commande à émettre, validation du paiement
 * fournisseur (le dossier passe à COMMANDEE) et bon de commande en PDF. La personne qui a décidé
 * d'un dossier ne peut pas valider son paiement fournisseur : le bouton ne lui est pas proposé et le
 * domaine refuse de toute façon. L'URL ne porte que l'identifiant technique de la demande.
 */
@Controller
class CommandesFournisseurBackOfficeController {

    static final String CHEMIN = "/bo/commandes";
    static final int LIMITE = 50;

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Logger LOG = LoggerFactory.getLogger(CommandesFournisseurBackOfficeController.class);

    private final CommandeFournisseurService commandes;
    private final Clock clock;

    CommandesFournisseurBackOfficeController(CommandeFournisseurService commandes, Clock clock) {
        this.commandes = commandes;
        this.clock = clock;
    }

    /** Dossier à commander tel qu'affiché. */
    record VueACommander(UUID demandeId, String numeroContrat, String designation, String fournisseur,
            BigDecimal montant, String depuis, String analyste, boolean apportEncaisse, boolean autorisationAcceptee,
            boolean validable, boolean decideParMoi) {
    }

    /** Commande validée telle qu'affichée. */
    record VueCommande(UUID demandeId, String numero, String numeroContrat, String designation, String fournisseur,
            BigDecimal montant, String validePar, String valideLe) {
    }

    @GetMapping(CHEMIN)
    String liste(Principal utilisateur, Model model) {
        String moi = utilisateur.getName();
        List<VueACommander> aCommander = commandes.aCommander().stream().map(d -> vue(d, moi)).toList();
        model.addAttribute("aCommander", aCommander);
        model.addAttribute("commandes", commandes.dernieres(LIMITE).stream().map(this::vue).toList());
        model.addAttribute("limite", LIMITE);
        return "bo/commandes/liste";
    }

    @PostMapping(CHEMIN + "/{id}/validation")
    String valider(@PathVariable("id") UUID id, Principal validateur, RedirectAttributes redirection) {
        try {
            CommandeFournisseur c = commandes.commander(id, validateur.getName());
            redirection.addFlashAttribute("succes", "bo.commandes.validee");
            redirection.addFlashAttribute("numero", c.numero());
        } catch (DossierIntrouvableException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (ErreurMetier e) {
            LOG.debug("Commande fournisseur non validée : {}", e.code());
            redirection.addFlashAttribute("erreur", e.code());
        }
        return "redirect:" + CHEMIN;
    }

    @GetMapping(CHEMIN + "/{id}/bon-de-commande.pdf")
    ResponseEntity<byte[]> bonDeCommande(@PathVariable("id") UUID id) {
        BonCommandePdf pdf = commandes.bonDeCommande(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.contenu().length)
                .header("Content-Disposition", ContentDisposition.inline().filename(pdf.nomFichier()).build()
                        .toString())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(pdf.contenu());
    }

    private VueACommander vue(DossierACommander d, String moi) {
        Demande demande = d.demande();
        return new VueACommander(demande.id(), d.numeroContrat(), demande.termes().designationProduit(),
                d.nomFournisseur(), demande.termes().prixAchat().valeur(),
                DATE_HEURE.format(demande.statutChangeLe().atZone(clock.getZone())), d.analyste(),
                d.prealables().apportEncaisse(), d.prealables().autorisationAcceptee(), d.validablePar(moi),
                d.analyste().equals(moi));
    }

    private VueCommande vue(LigneCommande l) {
        CommandeFournisseur c = l.commande();
        return new VueCommande(c.demandeId(), c.numero(), c.numeroContrat(), c.designationProduit(),
                l.nomFournisseur(), c.montant().valeur(), c.validePar(),
                DATE_HEURE.format(c.valideLe().atZone(clock.getZone())));
    }
}
