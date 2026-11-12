package com.cic.catalogue.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.cic.catalogue.application.CatalogueClientService;
import com.cic.catalogue.application.FournisseursEnMemoire;
import com.cic.catalogue.application.ProduitPropose;
import com.cic.catalogue.application.ProduitsEnMemoire;
import com.cic.catalogue.domain.CategorieProduit;
import com.cic.catalogue.domain.Fournisseur;
import com.cic.catalogue.domain.Produit;
import com.cic.commun.domain.Montant;
import com.cic.contrat.application.CalculPrixService;
import com.cic.contrat.application.EcheancierService;
import com.cic.contrat.application.SimulationService;
import com.cic.contrat.domain.Simulation;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class CatalogueClientControllerTest {

    private final ProduitsEnMemoire produits = new ProduitsEnMemoire();
    private final FournisseursEnMemoire fournisseurs = new FournisseursEnMemoire();
    private final CatalogueClientController controller = new CatalogueClientController(service());
    private final Fournisseur fournisseur = Fournisseur.nouveau("Mauritel", new BigDecimal("8"));
    private final Produit telephone = Produit.nouveau("TEL-001", "Galaxy A35", "Samsung",
            CategorieProduit.TELEPHONE, fournisseur.id(), Montant.de(20_000));
    private final Produit climatiseur = Produit.nouveau("ELM-001", "Climatiseur", "Gree",
            CategorieProduit.ELECTROMENAGER, fournisseur.id(), Montant.de(24_000));

    CatalogueClientControllerTest() {
        fournisseurs.enregistrer(fournisseur);
        produits.enregistrer(telephone);
        produits.enregistrer(climatiseur);
    }

    private CatalogueClientService service() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        CalculPrixService calculPrix = new CalculPrixService(parametres);
        return new CatalogueClientService(produits, fournisseurs, calculPrix,
                new SimulationService(calculPrix, new EcheancierService(parametres)), parametres);
    }

    @SuppressWarnings("unchecked")
    private static List<ProduitPropose> produitsAffiches(ExtendedModelMap model) {
        return (List<ProduitPropose>) model.get("produits");
    }

    @Test
    void affiche_tout_le_catalogue_sans_filtre() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.liste(null, model)).isEqualTo("client/catalogue/liste");
        assertThat(produitsAffiches(model)).extracting(ProduitPropose::designation)
                .containsExactly("Climatiseur", "Galaxy A35");
        assertThat(model.get("categorieChoisie")).isEqualTo("");
    }

    @Test
    void filtre_par_categorie() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.liste("TELEPHONE", model);

        assertThat(produitsAffiches(model)).extracting(ProduitPropose::designation).containsExactly("Galaxy A35");
        assertThat(model.get("categorieChoisie")).isEqualTo("TELEPHONE");
    }

    @Test
    void ignore_une_categorie_inconnue() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.liste("VOITURE", model);

        assertThat(produitsAffiches(model)).hasSize(2);
        assertThat(model.get("categorieChoisie")).isEqualTo("");
    }

    @Test
    void affiche_la_page_d_un_produit_avec_son_prix_de_vente() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.produit(telephone.id(), null, model)).isEqualTo("client/catalogue/produit");
        assertThat(((ProduitPropose) model.get("produit")).prixVente()).isEqualTo(Montant.de(20_800));
    }

    @Test
    void un_produit_indisponible_ou_inconnu_repond_404() {
        climatiseur.rendreIndisponible();
        produits.enregistrer(climatiseur);

        for (UUID id : List.of(climatiseur.id(), UUID.randomUUID())) {
            assertThatThrownBy(() -> controller.produit(id, null, new ExtendedModelMap()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Test
    void la_page_produit_simule_par_defaut_sur_la_duree_maximale() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.produit(telephone.id(), null, model);

        Simulation simulation = (Simulation) model.get("simulation");
        assertThat(simulation.nombreMois()).isEqualTo(6);
        assertThat(simulation.mensualite()).isEqualTo(Montant.de(2_080));
        assertThat(model.get("durees")).isEqualTo(List.of(3, 4, 5, 6));
    }

    @Test
    void la_page_produit_retient_la_duree_demandee_ou_la_maximale_si_hors_bornes() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.produit(telephone.id(), 4, model);
        assertThat(((Simulation) model.get("simulation")).nombreMois()).isEqualTo(4);

        ExtendedModelMap horsBornes = new ExtendedModelMap();
        controller.produit(telephone.id(), 12, horsBornes);
        assertThat(((Simulation) horsBornes.get("simulation")).nombreMois()).isEqualTo(6);
    }

    @Test
    void le_fragment_htmx_recalcule_la_simulation_pour_la_duree_choisie() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.simulation(telephone.id(), 3, model))
                .isEqualTo("client/catalogue/simulation :: resultat");
        Simulation simulation = (Simulation) model.get("simulation");
        assertThat(simulation.prixTotal()).isEqualTo(Montant.de(20_800));
        assertThat(simulation.apport()).isEqualTo(Montant.de(8_320));
        assertThat(simulation.mensualite()).isEqualTo(Montant.de(4_160));
    }

    @Test
    void le_fragment_refuse_une_duree_hors_bornes_et_un_produit_inconnu() {
        assertThatThrownBy(() -> controller.simulation(telephone.id(), 7, new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> controller.simulation(UUID.randomUUID(), 6, new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
