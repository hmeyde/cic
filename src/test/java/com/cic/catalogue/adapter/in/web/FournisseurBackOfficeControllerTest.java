package com.cic.catalogue.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.cic.catalogue.application.FournisseurService;
import com.cic.catalogue.application.FournisseursEnMemoire;
import com.cic.catalogue.domain.Fournisseur;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class FournisseurBackOfficeControllerTest {

    private final FournisseursEnMemoire repository = new FournisseursEnMemoire();
    private final FournisseurBackOfficeController controller = new FournisseurBackOfficeController(service());

    private FournisseurService service() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        return new FournisseurService(repository, parametres);
    }

    private static FournisseurFormulaire formulaire(String nom, String remise, boolean actif) {
        FournisseurFormulaire f = new FournisseurFormulaire();
        f.setNom(nom);
        f.setRemise(remise);
        f.setActif(actif);
        return f;
    }

    private static BindingResult erreurs(FournisseurFormulaire f) {
        return new BeanPropertyBindingResult(f, "formulaire");
    }

    @Test
    void cree_un_fournisseur_puis_revient_a_la_liste() {
        FournisseurFormulaire saisie = formulaire("Mauritel", "7,5", true);
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.creer(saisie, erreurs(saisie), new ExtendedModelMap(), redirection);

        assertThat(vue).isEqualTo("redirect:/bo/fournisseurs");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("bo.fournisseur.cree");
        assertThat(repository.tous()).singleElement().satisfies(f -> {
            assertThat(f.nom()).isEqualTo("Mauritel");
            assertThat(f.remiseNegocieePct()).isEqualByComparingTo("7.5");
        });
    }

    @Test
    void reaffiche_le_formulaire_si_la_remise_est_hors_bornes() {
        FournisseurFormulaire saisie = formulaire("Mauritel", "12", true);
        BindingResult erreurs = erreurs(saisie);
        ExtendedModelMap model = new ExtendedModelMap();

        String vue = controller.creer(saisie, erreurs, model, new RedirectAttributesModelMap());

        assertThat(vue).isEqualTo("bo/fournisseurs/formulaire");
        assertThat(erreurs.getFieldError("remise").getCode()).isEqualTo("bo.fournisseur.remise.hors.bornes");
        assertThat(erreurs.getFieldError("remise").getArguments()).containsExactly("5", "10");
        assertThat(model).containsEntry("remiseMin", "5").containsEntry("remiseMax", "10");
        assertThat(repository.tous()).isEmpty();
    }

    @Test
    void reaffiche_le_formulaire_si_le_nom_est_pris() {
        repository.enregistrer(Fournisseur.nouveau("Mauritel", new BigDecimal("8")));
        FournisseurFormulaire saisie = formulaire("MAURITEL", "8", true);
        BindingResult erreurs = erreurs(saisie);

        String vue = controller.creer(saisie, erreurs, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(vue).isEqualTo("bo/fournisseurs/formulaire");
        assertThat(erreurs.getFieldError("nom").getCode()).isEqualTo("bo.fournisseur.nom.deja.utilise");
    }

    @Test
    void preremplit_puis_enregistre_la_modification() {
        Fournisseur existant = Fournisseur.nouveau("Mauritel", new BigDecimal("8"));
        repository.enregistrer(existant);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.modification(existant.id(), model)).isEqualTo("bo/fournisseurs/formulaire");
        assertThat(model.get("fournisseurId")).isEqualTo(existant.id());
        assertThat(((FournisseurFormulaire) model.get("formulaire")).getRemise()).isEqualTo("8");

        FournisseurFormulaire saisie = formulaire("Mauritel Distribution", "6", false);
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();
        String vue = controller.modifier(existant.id(), saisie, erreurs(saisie), new ExtendedModelMap(), redirection);

        assertThat(vue).isEqualTo("redirect:/bo/fournisseurs");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("bo.fournisseur.modifie");
        Fournisseur relu = repository.parId(existant.id()).orElseThrow();
        assertThat(relu.nom()).isEqualTo("Mauritel Distribution");
        assertThat(relu.remiseNegocieePct()).isEqualByComparingTo("6");
        assertThat(relu.estActif()).isFalse();
    }

    @Test
    void un_fournisseur_inconnu_donne_une_page_introuvable() {
        UUID inconnu = UUID.randomUUID();
        FournisseurFormulaire saisie = formulaire("A", "8", true);

        assertThatThrownBy(() -> controller.modification(inconnu, new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> controller.modifier(inconnu, saisie, erreurs(saisie), new ExtendedModelMap(),
                new RedirectAttributesModelMap()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
