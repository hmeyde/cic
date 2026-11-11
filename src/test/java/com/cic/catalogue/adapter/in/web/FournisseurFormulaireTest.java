package com.cic.catalogue.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.cic.catalogue.domain.Fournisseur;

class FournisseurFormulaireTest {

    private static FournisseurFormulaire formulaire(String nom, String remise) {
        FournisseurFormulaire f = new FournisseurFormulaire();
        f.setNom(nom);
        f.setRemise(remise);
        return f;
    }

    private static Errors valider(FournisseurFormulaire f) {
        Errors erreurs = new BeanPropertyBindingResult(f, "formulaire");
        f.valider(erreurs);
        return erreurs;
    }

    @Test
    void accepte_la_virgule_ou_le_point_decimal() {
        assertThat(valider(formulaire("A", "7,5")).hasErrors()).isFalse();
        assertThat(formulaire("A", " 7,5 ").remisePct()).contains(new BigDecimal("7.5"));
        assertThat(formulaire("A", "8.25").remisePct()).contains(new BigDecimal("8.25"));
        assertThat(formulaire("A", "8").remisePct()).contains(new BigDecimal("8"));
    }

    @Test
    void refuse_les_champs_vides() {
        Errors erreurs = valider(formulaire("  ", ""));

        assertThat(erreurs.getFieldError("nom").getCode()).isEqualTo("bo.formulaire.obligatoire");
        assertThat(erreurs.getFieldError("remise").getCode()).isEqualTo("bo.formulaire.obligatoire");
    }

    @Test
    void refuse_un_nom_trop_long() {
        Errors erreurs = valider(formulaire("x".repeat(151), "8"));

        assertThat(erreurs.getFieldError("nom").getCode()).isEqualTo("bo.formulaire.trop.long");
        assertThat(valider(formulaire("x".repeat(150), "8")).hasErrors()).isFalse();
    }

    @Test
    void refuse_une_remise_mal_formee() {
        for (String saisie : new String[] {"abc", "-5", "100", "7.125", "8 %", "1e1"}) {
            Errors erreurs = valider(formulaire("A", saisie));
            assertThat(erreurs.getFieldError("remise").getCode()).as(saisie).isEqualTo("bo.fournisseur.remise.invalide");
            assertThat(formulaire("A", saisie).remisePct()).as(saisie).isEmpty();
        }
    }

    @Test
    void se_remplit_depuis_un_fournisseur() {
        Fournisseur f = Fournisseur.reconstituer(UUID.randomUUID(), "Mauritel", new BigDecimal("7.50"), false);

        FournisseurFormulaire formulaire = FournisseurFormulaire.de(f);

        assertThat(formulaire.getNom()).isEqualTo("Mauritel");
        assertThat(formulaire.getRemise()).isEqualTo("7.5");
        assertThat(formulaire.isActif()).isFalse();
    }
}
