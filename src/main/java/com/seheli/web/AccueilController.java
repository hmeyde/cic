package com.seheli.web;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.SimulationService;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametres;

/**
 * Page d'accueil publique, avec les balises Open Graph (aperçu du lien partagé : titre, description,
 * logo). Open Graph exige des URL absolues : elles sont construites depuis la requête courante.
 *
 * <p>L'exemple chiffré est calculé par le domaine ({@link SimulationService}) avec les paramètres en
 * vigueur : produit de référence à {@value #PRIX_PUBLIC_EXEMPLE} MRU (remise fournisseur de
 * {@value #REMISE_EXEMPLE_PCT} %, sans effet sur ce que paie le client), sur la durée maximale.
 */
@Controller
class AccueilController {

    static final long PRIX_PUBLIC_EXEMPLE = 20_000;
    static final String REMISE_EXEMPLE_PCT = "8";

    private final SimulationService simulation;
    private final ParametreService parametres;

    AccueilController(SimulationService simulation, ParametreService parametres) {
        this.simulation = simulation;
        this.parametres = parametres;
    }

    @GetMapping("/")
    String accueil(Model model) {
        model.addAttribute("exemple", exemple());
        model.addAttribute("ogTitre", "accueil.titre-page");
        model.addAttribute("ogUrl", ServletUriComponentsBuilder.fromCurrentContextPath().path("/").toUriString());
        model.addAttribute("ogImage",
                ServletUriComponentsBuilder.fromCurrentContextPath().path("/img/og-seheli.png").toUriString());
        return "accueil";
    }

    ExempleAccueil exemple() {
        Parametres p = parametres.courants();
        int dureeMax = p.entier(CleParametre.DUREE_MAX_MOIS);
        return new ExempleAccueil(
                p.pourcentage(CleParametre.APPORT_MINIMUM_PCT),
                p.entier(CleParametre.DUREE_MIN_MOIS),
                dureeMax,
                simulation.simuler(Montant.de(PRIX_PUBLIC_EXEMPLE), new BigDecimal(REMISE_EXEMPLE_PCT), dureeMax));
    }
}
