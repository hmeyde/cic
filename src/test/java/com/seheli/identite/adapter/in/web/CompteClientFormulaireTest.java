package com.seheli.identite.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

class CompteClientFormulaireTest {

    static CompteClientFormulaire formulaire(String nni, String nom, String prenom, String date, boolean consentement) {
        CompteClientFormulaire f = new CompteClientFormulaire();
        f.setNni(nni);
        f.setNom(nom);
        f.setPrenom(prenom);
        f.setDateNaissance(date);
        f.setConsentement(consentement);
        return f;
    }

    private static Errors valider(CompteClientFormulaire f) {
        Errors erreurs = new BeanPropertyBindingResult(f, "formulaire");
        f.valider(erreurs);
        return erreurs;
    }

    @Test
    void une_saisie_complete_donne_la_commande() {
        CompteClientFormulaire f = formulaire(" 12345 67890 ", " Ould Ahmed ", "Sidi", "1990-01-15", true);

        assertThat(valider(f).hasErrors()).isFalse();
        assertThat(f.versCommande("22123456")).satisfies(c -> {
            assertThat(c.telephone()).isEqualTo("22123456");
            assertThat(c.nni()).isEqualTo("1234567890");
            assertThat(c.nom()).isEqualTo("Ould Ahmed");
            assertThat(c.dateNaissance()).isEqualTo(LocalDate.of(1990, 1, 15));
            assertThat(c.consentement()).isTrue();
        });
    }

    @Test
    void refuse_les_champs_vides_et_l_absence_de_consentement() {
        Errors erreurs = valider(formulaire("", " ", null, "", false));

        assertThat(erreurs.getFieldError("nni").getCode()).isEqualTo("client.compte.obligatoire");
        assertThat(erreurs.getFieldError("nom").getCode()).isEqualTo("client.compte.obligatoire");
        assertThat(erreurs.getFieldError("prenom").getCode()).isEqualTo("client.compte.obligatoire");
        assertThat(erreurs.getFieldError("dateNaissance").getCode()).isEqualTo("client.compte.obligatoire");
        assertThat(erreurs.getFieldError("consentement").getCode()).isEqualTo("client.compte.consentement.manquant");
    }

    @Test
    void refuse_un_nni_ou_une_date_mal_formes_et_un_nom_trop_long() {
        Errors erreurs = valider(formulaire("12345A7890", "x".repeat(101), "Sidi", "15/01/1990", true));

        assertThat(erreurs.getFieldError("nni").getCode()).isEqualTo("client.compte.nni.invalide");
        assertThat(erreurs.getFieldError("nom").getCode()).isEqualTo("client.compte.trop.long");
        assertThat(erreurs.getFieldError("dateNaissance").getCode()).isEqualTo("client.compte.date-naissance.invalide");
    }
}
