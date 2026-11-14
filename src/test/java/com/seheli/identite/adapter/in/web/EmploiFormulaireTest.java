package com.seheli.identite.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.seheli.commun.domain.Montant;
import com.seheli.identite.domain.Emploi;
import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeContrat;

class EmploiFormulaireTest {

    static EmploiFormulaire formulaire(String type, String debut, String fin, String salaire, String jour,
            boolean compte, boolean bmci, String mode) {
        EmploiFormulaire f = new EmploiFormulaire();
        f.setEmployeur(" SNIM ");
        f.setTypeContrat(type);
        f.setDateDebut(debut);
        f.setDateFin(fin);
        f.setSalaireNet(salaire);
        f.setJourPaie(jour);
        f.setSalaireVerseSurCompte(compte);
        f.setSalaireDomicilieBmci(bmci);
        f.setModeRemboursement(mode);
        return f;
    }

    static EmploiFormulaire cdiValide() {
        return formulaire("CDI", "2022-03-01", "", "30 000", "25", true, true, "DOMICILIATION_BMCI");
    }

    private static Errors valider(EmploiFormulaire f) {
        Errors erreurs = new BeanPropertyBindingResult(f, "formulaire");
        f.valider(erreurs);
        return erreurs;
    }

    @Test
    void une_saisie_complete_donne_la_commande() {
        EmploiFormulaire f = cdiValide();

        assertThat(valider(f).hasErrors()).isFalse();
        assertThat(f.versCommande()).satisfies(c -> {
            assertThat(c.employeur()).isEqualTo("SNIM");
            assertThat(c.typeContrat()).isEqualTo(TypeContrat.CDI);
            assertThat(c.dateDebut()).isEqualTo(LocalDate.of(2022, 3, 1));
            assertThat(c.dateFin()).isNull();
            assertThat(c.salaireNet()).isEqualTo(Montant.de(30_000));
            assertThat(c.jourPaie()).isEqualTo(25);
            assertThat(c.modeRemboursement()).isEqualTo(ModeRemboursement.DOMICILIATION_BMCI);
        });
    }

    @Test
    void la_date_de_fin_n_est_lue_que_pour_un_cdd() {
        EmploiFormulaire cdi = formulaire("CDI", "2022-03-01", "2027-01-01", "30000", "25", false, false,
                "RETENUE_EMPLOYEUR");
        assertThat(valider(cdi).hasErrors()).isFalse();
        assertThat(cdi.versCommande().dateFin()).isNull();

        EmploiFormulaire cddSansFin = formulaire("CDD", "2022-03-01", "", "30000", "25", false, false,
                "RETENUE_EMPLOYEUR");
        assertThat(valider(cddSansFin).getFieldError("dateFin").getCode())
                .isEqualTo("client.emploi.date-fin.obligatoire");

        EmploiFormulaire cddInverse = formulaire("CDD", "2022-03-01", "2022-03-01", "30000", "25", false, false,
                "RETENUE_EMPLOYEUR");
        assertThat(valider(cddInverse).getFieldError("dateFin").getCode())
                .isEqualTo("client.emploi.date-fin.avant-debut");

        EmploiFormulaire cdd = formulaire("CDD", "2022-03-01", "2027-06-30", "30000", "25", false, false,
                "RETENUE_EMPLOYEUR");
        assertThat(valider(cdd).hasErrors()).isFalse();
        assertThat(cdd.versCommande().dateFin()).isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    void signale_les_champs_vides_ou_mal_formes() {
        EmploiFormulaire vide = formulaire("", "", "", "", "", false, false, "");
        vide.setEmployeur("  ");
        Errors erreurs = valider(vide);
        assertThat(erreurs.getFieldErrors()).extracting(e -> e.getField())
                .containsExactlyInAnyOrder("employeur", "typeContrat", "dateDebut", "salaireNet", "jourPaie",
                        "modeRemboursement");

        Errors malFormes = valider(formulaire("INTERIM", "01/03/2022", "", "30000,5", "32", true, false,
                "RETENUE_EMPLOYEUR"));
        assertThat(malFormes.getFieldError("typeContrat").getCode()).isEqualTo("client.emploi.choix.obligatoire");
        assertThat(malFormes.getFieldError("dateDebut").getCode()).isEqualTo("client.emploi.date.invalide");
        assertThat(malFormes.getFieldError("salaireNet").getCode()).isEqualTo("client.emploi.salaire.invalide");
        assertThat(malFormes.getFieldError("jourPaie").getCode()).isEqualTo("client.emploi.jour-paie.invalide");

        assertThat(valider(formulaire("CDI", "2022-03-01", "", "0", "1", true, false, "RETENUE_EMPLOYEUR"))
                .getFieldError("salaireNet").getCode()).isEqualTo("client.emploi.salaire.invalide");
        assertThat(valider(formulaire("CDI", "2022-03-01", "", "1234567890123", "1", true, false,
                "RETENUE_EMPLOYEUR")).getFieldError("salaireNet").getCode())
                .isEqualTo("client.emploi.salaire.invalide");
    }

    @Test
    void le_mode_de_remboursement_doit_suivre_le_versement_du_salaire() {
        assertThat(valider(formulaire("CDI", "2022-03-01", "", "30000", "25", false, true, "RETENUE_EMPLOYEUR"))
                .getFieldError("salaireDomicilieBmci").getCode()).isEqualTo("client.emploi.bmci.sans-compte");
        assertThat(valider(formulaire("CDI", "2022-03-01", "", "30000", "25", true, false, "DOMICILIATION_BMCI"))
                .getFieldError("modeRemboursement").getCode())
                .isEqualTo("client.emploi.mode.domiciliation-sans-bmci");
        assertThat(valider(formulaire("CDI", "2022-03-01", "", "30000", "25", false, false, "VIREMENT_PERMANENT"))
                .getFieldError("modeRemboursement").getCode()).isEqualTo("client.emploi.mode.virement-sans-compte");
    }

    @Test
    void se_preremplit_depuis_l_emploi_enregistre() {
        Emploi e = Emploi.nouveau(java.util.UUID.randomUUID(), "SNIM", TypeContrat.CDD, LocalDate.of(2025, 1, 1),
                LocalDate.of(2027, 12, 31), Montant.de(25_000), 28, true, false, ModeRemboursement.VIREMENT_PERMANENT);

        EmploiFormulaire f = EmploiFormulaire.depuis(e);

        assertThat(f.getEmployeur()).isEqualTo("SNIM");
        assertThat(f.getTypeContrat()).isEqualTo("CDD");
        assertThat(f.getDateDebut()).isEqualTo("2025-01-01");
        assertThat(f.getDateFin()).isEqualTo("2027-12-31");
        assertThat(f.getSalaireNet()).isEqualTo("25000");
        assertThat(f.getJourPaie()).isEqualTo("28");
        assertThat(f.getModeRemboursement()).isEqualTo("VIREMENT_PERMANENT");
        assertThat(valider(f).hasErrors()).isFalse();
    }
}
