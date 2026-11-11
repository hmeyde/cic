package com.cic.catalogue.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;

import com.cic.catalogue.domain.CategorieProduit;
import com.cic.commun.domain.Montant;

class ProduitFormulaireTest {

    private static ProduitFormulaire valide() {
        ProduitFormulaire f = new ProduitFormulaire();
        f.setReference("TEL-001");
        f.setDesignation("Galaxy A15");
        f.setMarque("Samsung");
        f.setCategorie("TELEPHONE");
        f.setFournisseurId(UUID.randomUUID().toString());
        f.setPrixPublic("20 000");
        return f;
    }

    private static BeanPropertyBindingResult valider(ProduitFormulaire f, boolean creation) {
        BeanPropertyBindingResult erreurs = new BeanPropertyBindingResult(f, "formulaire");
        f.valider(erreurs, creation);
        return erreurs;
    }

    @Test
    void une_saisie_complete_est_valide() {
        ProduitFormulaire f = valide();

        assertThat(valider(f, true).hasErrors()).isFalse();
        assertThat(f.prixSaisi()).contains(Montant.de(20_000));
        assertThat(f.categorieChoisie()).contains(CategorieProduit.TELEPHONE);
    }

    @Test
    void les_champs_sont_obligatoires() {
        ProduitFormulaire f = new ProduitFormulaire();

        BeanPropertyBindingResult erreurs = valider(f, true);

        assertThat(erreurs.getFieldErrors()).extracting(e -> e.getField())
                .containsExactlyInAnyOrder("reference", "designation", "marque", "categorie", "fournisseurId",
                        "prixPublic");
        assertThat(erreurs.getFieldError("reference").getCode()).isEqualTo("bo.formulaire.obligatoire");
    }

    @Test
    void la_reference_n_est_pas_verifiee_en_modification() {
        ProduitFormulaire f = valide();
        f.setReference("");

        assertThat(valider(f, false).hasErrors()).isFalse();
    }

    @Test
    void le_prix_doit_etre_un_entier_positif() {
        for (String prix : new String[] {"0", "-5", "20000,50", "abc", "1234567890123"}) {
            ProduitFormulaire f = valide();
            f.setPrixPublic(prix);

            assertThat(valider(f, true).getFieldError("prixPublic").getCode())
                    .as(prix).isEqualTo("bo.produit.prix.invalide");
        }
    }

    @Test
    void une_categorie_ou_un_fournisseur_inconnus_sont_refuses() {
        ProduitFormulaire f = valide();
        f.setCategorie("VOITURE");
        f.setFournisseurId("pas-un-uuid");

        BeanPropertyBindingResult erreurs = valider(f, true);

        assertThat(erreurs.getFieldError("categorie")).isNotNull();
        assertThat(erreurs.getFieldError("fournisseurId")).isNotNull();
    }

    @Test
    void une_designation_trop_longue_est_refusee() {
        ProduitFormulaire f = valide();
        f.setDesignation("x".repeat(201));

        assertThat(valider(f, true).getFieldError("designation").getCode()).isEqualTo("bo.formulaire.trop.long");
    }
}
