package com.seheli.catalogue.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.catalogue.application.FournisseurService;
import com.seheli.catalogue.application.FournisseursEnMemoire;
import com.seheli.catalogue.application.ProduitService;
import com.seheli.catalogue.application.ProduitsEnMemoire;
import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.catalogue.domain.Produit;
import com.seheli.commun.domain.Montant;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class ProduitBackOfficeControllerTest {

    private final ProduitsEnMemoire produits = new ProduitsEnMemoire();
    private final FournisseursEnMemoire fournisseurs = new FournisseursEnMemoire();
    private final ProduitBackOfficeController controller = new ProduitBackOfficeController(
            new ProduitService(produits, fournisseurs), fournisseurService());

    private FournisseurService fournisseurService() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        return new FournisseurService(fournisseurs, parametres);
    }

    private Fournisseur fournisseur(String nom, boolean actif) {
        Fournisseur f = Fournisseur.nouveau(nom, new BigDecimal("8"));
        if (!actif) {
            f.desactiver();
        }
        fournisseurs.enregistrer(f);
        return f;
    }

    private static ProduitFormulaire formulaire(String reference, UUID fournisseurId, String prix, boolean disponible) {
        ProduitFormulaire f = new ProduitFormulaire();
        f.setReference(reference);
        f.setDesignation("Galaxy A15");
        f.setMarque("Samsung");
        f.setCategorie("TELEPHONE");
        f.setFournisseurId(fournisseurId.toString());
        f.setPrixPublic(prix);
        f.setDisponible(disponible);
        return f;
    }

    private static BindingResult erreurs(ProduitFormulaire f) {
        return new BeanPropertyBindingResult(f, "formulaire");
    }

    @Test
    void cree_un_produit_puis_revient_a_la_liste() {
        Fournisseur f = fournisseur("Mauritel", true);
        ProduitFormulaire saisie = formulaire("TEL-001", f.id(), "20 000", true);
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.creer(saisie, erreurs(saisie), new ExtendedModelMap(), redirection);

        assertThat(vue).isEqualTo("redirect:/bo/produits");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("bo.produit.cree");
        assertThat(produits.tous()).singleElement().satisfies(p -> {
            assertThat(p.reference()).isEqualTo("TEL-001");
            assertThat(p.prixPublic()).isEqualTo(Montant.de(20_000));
            assertThat(p.estDisponible()).isTrue();
        });
    }

    @Test
    void le_formulaire_ne_propose_que_les_fournisseurs_actifs() {
        Fournisseur actif = fournisseur("Mauritel", true);
        fournisseur("Ancien", false);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.nouveau(model)).isEqualTo("bo/produits/formulaire");
        @SuppressWarnings("unchecked")
        List<Fournisseur> choix = (List<Fournisseur>) model.get("fournisseurs");
        assertThat(choix).extracting(Fournisseur::id).containsExactly(actif.id());
        assertThat(model.get("produitId")).isNull();
    }

    @Test
    void reaffiche_le_formulaire_si_la_reference_est_prise() {
        Fournisseur f = fournisseur("Mauritel", true);
        produits.enregistrer(Produit.nouveau("TEL-001", "A", "B", CategorieProduit.TELEPHONE, f.id(), Montant.de(1)));
        ProduitFormulaire saisie = formulaire("TEL-001", f.id(), "20000", true);
        BindingResult erreurs = erreurs(saisie);

        String vue = controller.creer(saisie, erreurs, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(vue).isEqualTo("bo/produits/formulaire");
        assertThat(erreurs.getFieldError("reference").getCode()).isEqualTo("bo.produit.reference.deja.utilisee");
    }

    @Test
    void reaffiche_le_formulaire_si_le_fournisseur_est_inactif() {
        Fournisseur inactif = fournisseur("Ancien", false);
        ProduitFormulaire saisie = formulaire("TEL-001", inactif.id(), "20000", true);
        BindingResult erreurs = erreurs(saisie);

        String vue = controller.creer(saisie, erreurs, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(vue).isEqualTo("bo/produits/formulaire");
        assertThat(erreurs.getFieldError("fournisseurId").getCode()).isEqualTo("bo.produit.fournisseur.inactif");
        assertThat(produits.tous()).isEmpty();
    }

    @Test
    void preremplit_puis_enregistre_la_modification_sans_changer_la_reference() {
        Fournisseur f = fournisseur("Mauritel", true);
        Produit existant = Produit.nouveau("TEL-001", "Galaxy", "Samsung", CategorieProduit.TELEPHONE, f.id(),
                Montant.de(20_000));
        produits.enregistrer(existant);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.modification(existant.id(), model)).isEqualTo("bo/produits/formulaire");
        assertThat(model.get("produitId")).isEqualTo(existant.id());
        assertThat(((ProduitFormulaire) model.get("formulaire")).getPrixPublic()).isEqualTo("20000");

        ProduitFormulaire saisie = formulaire("AUTRE-REF", f.id(), "21500", false);
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();
        String vue = controller.modifier(existant.id(), saisie, erreurs(saisie), new ExtendedModelMap(), redirection);

        assertThat(vue).isEqualTo("redirect:/bo/produits");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("bo.produit.modifie");
        Produit relu = produits.parId(existant.id()).orElseThrow();
        assertThat(relu.reference()).isEqualTo("TEL-001");
        assertThat(relu.designation()).isEqualTo("Galaxy A15");
        assertThat(relu.prixPublic()).isEqualTo(Montant.de(21_500));
        assertThat(relu.estDisponible()).isFalse();
    }

    @Test
    void rend_un_produit_indisponible_depuis_la_liste() {
        Fournisseur f = fournisseur("Mauritel", true);
        Produit p = Produit.nouveau("TEL-001", "Galaxy", "Samsung", CategorieProduit.TELEPHONE, f.id(),
                Montant.de(20_000));
        produits.enregistrer(p);
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.changerDisponibilite(p.id(), false, redirection);

        assertThat(vue).isEqualTo("redirect:/bo/produits");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("bo.produit.rendu.indisponible");
        assertThat(produits.parId(p.id()).orElseThrow().estDisponible()).isFalse();
    }

    @Test
    void un_produit_inconnu_donne_une_page_introuvable() {
        UUID inconnu = UUID.randomUUID();

        assertThatThrownBy(() -> controller.modification(inconnu, new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> controller.changerDisponibilite(inconnu, false, new RedirectAttributesModelMap()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
