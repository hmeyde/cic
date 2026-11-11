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
        return new CatalogueClientService(produits, fournisseurs, new CalculPrixService(parametres), parametres);
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

        assertThat(controller.produit(telephone.id(), model)).isEqualTo("client/catalogue/produit");
        assertThat(((ProduitPropose) model.get("produit")).prixVente()).isEqualTo(Montant.de(20_800));
    }

    @Test
    void un_produit_indisponible_ou_inconnu_repond_404() {
        climatiseur.rendreIndisponible();
        produits.enregistrer(climatiseur);

        for (UUID id : List.of(climatiseur.id(), UUID.randomUUID())) {
            assertThatThrownBy(() -> controller.produit(id, new ExtendedModelMap()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }
}
