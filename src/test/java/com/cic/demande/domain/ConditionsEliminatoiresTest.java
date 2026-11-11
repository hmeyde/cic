package com.cic.demande.domain;

import static com.cic.demande.domain.ConditionEliminatoire.AGE_MINIMUM;
import static com.cic.demande.domain.ConditionEliminatoire.AUCUN_RETARD;
import static com.cic.demande.domain.ConditionEliminatoire.CONTRAT_EN_COURS;
import static com.cic.demande.domain.ConditionEliminatoire.ENDETTEMENT;
import static com.cic.demande.domain.ConditionEliminatoire.IDENTITE_VERIFIEE;
import static com.cic.demande.domain.ConditionEliminatoire.SALAIRE_SUR_COMPTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Fraction;
import com.cic.commun.domain.Montant;

class ConditionsEliminatoiresTest {

    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 11);
    private static final ReglesEligibilite REGLES = new ReglesEligibilite(18, Fraction.de("1/3"));

    /** Dossier qui satisfait toutes les conditions : salaire 30 000, mensualité 2 080 (exemple chiffré). */
    private static final DossierEligibilite VALIDE = new DossierEligibilite(
            true, true,
            LocalDate.of(1990, 5, 1),
            LocalDate.of(2020, 1, 1), Optional.empty(),
            true,
            Montant.de(30_000), Montant.ZERO, Montant.de(2_080),
            false);

    private static Eligibilite evaluer(DossierEligibilite d) {
        return ConditionsEliminatoires.evaluer(d, REGLES, AUJOURD_HUI);
    }

    private static DossierEligibilite identite(boolean nni, boolean piece) {
        DossierEligibilite v = VALIDE;
        return new DossierEligibilite(nni, piece, v.dateNaissance(), v.debutContrat(), v.finContrat(),
                v.salaireVerseSurCompte(), v.salaireNet(), v.mensualitesCicEnCours(), v.nouvelleMensualite(),
                v.retardEnCours());
    }

    private static DossierEligibilite neLe(LocalDate naissance) {
        DossierEligibilite v = VALIDE;
        return new DossierEligibilite(v.nniVerifie(), v.pieceIdentiteVerifiee(), naissance, v.debutContrat(),
                v.finContrat(), v.salaireVerseSurCompte(), v.salaireNet(), v.mensualitesCicEnCours(),
                v.nouvelleMensualite(), v.retardEnCours());
    }

    private static DossierEligibilite contrat(LocalDate debut, Optional<LocalDate> fin) {
        DossierEligibilite v = VALIDE;
        return new DossierEligibilite(v.nniVerifie(), v.pieceIdentiteVerifiee(), v.dateNaissance(), debut, fin,
                v.salaireVerseSurCompte(), v.salaireNet(), v.mensualitesCicEnCours(), v.nouvelleMensualite(),
                v.retardEnCours());
    }

    private static DossierEligibilite salaireSurCompte(boolean surCompte) {
        DossierEligibilite v = VALIDE;
        return new DossierEligibilite(v.nniVerifie(), v.pieceIdentiteVerifiee(), v.dateNaissance(),
                v.debutContrat(), v.finContrat(), surCompte, v.salaireNet(), v.mensualitesCicEnCours(),
                v.nouvelleMensualite(), v.retardEnCours());
    }

    private static DossierEligibilite endettement(long salaire, long enCours, long nouvelle) {
        DossierEligibilite v = VALIDE;
        return new DossierEligibilite(v.nniVerifie(), v.pieceIdentiteVerifiee(), v.dateNaissance(),
                v.debutContrat(), v.finContrat(), v.salaireVerseSurCompte(), Montant.de(salaire),
                Montant.de(enCours), Montant.de(nouvelle), v.retardEnCours());
    }

    private static DossierEligibilite retard(boolean enRetard) {
        DossierEligibilite v = VALIDE;
        return new DossierEligibilite(v.nniVerifie(), v.pieceIdentiteVerifiee(), v.dateNaissance(),
                v.debutContrat(), v.finContrat(), v.salaireVerseSurCompte(), v.salaireNet(),
                v.mensualitesCicEnCours(), v.nouvelleMensualite(), enRetard);
    }

    @Test
    void un_dossier_complet_est_eligible_et_chaque_condition_a_une_raison() {
        Eligibilite e = evaluer(VALIDE);

        assertThat(e.estEligible()).isTrue();
        assertThat(e.nonSatisfaites()).isEmpty();
        assertThat(e.resultats()).extracting(ResultatCondition::condition)
                .containsExactly(ConditionEliminatoire.values());
        assertThat(e.resultats()).allSatisfy(r -> assertThat(r.raison()).isNotBlank());
    }

    @Test
    void toutes_les_conditions_sont_evaluees_sans_s_arreter_a_la_premiere() {
        DossierEligibilite d = new DossierEligibilite(false, false, LocalDate.of(2010, 1, 1),
                LocalDate.of(2020, 1, 1), Optional.of(LocalDate.of(2026, 1, 1)), false,
                Montant.de(3_000), Montant.ZERO, Montant.de(2_080), true);

        Eligibilite e = evaluer(d);

        assertThat(e.estEligible()).isFalse();
        assertThat(e.nonSatisfaites()).extracting(ResultatCondition::condition)
                .containsExactly(ConditionEliminatoire.values());
    }

    @Nested
    class Identite {

        @Test
        void nni_et_piece_verifies() {
            assertThat(evaluer(identite(true, true)).resultat(IDENTITE_VERIFIEE).satisfaite()).isTrue();
        }

        @Test
        void nni_non_verifie() {
            ResultatCondition r = evaluer(identite(false, true)).resultat(IDENTITE_VERIFIEE);
            assertThat(r.satisfaite()).isFalse();
            assertThat(r.raison()).isEqualTo("NNI non vérifié");
        }

        @Test
        void piece_non_verifiee() {
            ResultatCondition r = evaluer(identite(true, false)).resultat(IDENTITE_VERIFIEE);
            assertThat(r.satisfaite()).isFalse();
            assertThat(r.raison()).isEqualTo("Pièce d'identité non vérifiée");
        }

        @Test
        void ni_l_un_ni_l_autre() {
            Eligibilite e = evaluer(identite(false, false));
            assertThat(e.estEligible()).isFalse();
            assertThat(e.nonSatisfaites()).extracting(ResultatCondition::condition).containsExactly(IDENTITE_VERIFIEE);
        }
    }

    @Nested
    class Age {

        @Test
        void dix_huit_ans_le_jour_meme() {
            ResultatCondition r = evaluer(neLe(LocalDate.of(2008, 11, 11))).resultat(AGE_MINIMUM);
            assertThat(r.satisfaite()).isTrue();
            assertThat(r.raison()).startsWith("18 ans");
        }

        @Test
        void dix_huit_ans_le_lendemain() {
            ResultatCondition r = evaluer(neLe(LocalDate.of(2008, 11, 12))).resultat(AGE_MINIMUM);
            assertThat(r.satisfaite()).isFalse();
            assertThat(r.raison()).startsWith("17 ans");
        }

        @Test
        void ne_un_29_fevrier_majeur_le_1er_mars() {
            ReglesEligibilite regles = new ReglesEligibilite(18, Fraction.de("1/3"));
            DossierEligibilite d = neLe(LocalDate.of(2008, 2, 29));
            assertThat(ConditionsEliminatoires.evaluer(d, regles, LocalDate.of(2026, 2, 28))
                    .resultat(AGE_MINIMUM).satisfaite()).isFalse();
            assertThat(ConditionsEliminatoires.evaluer(d, regles, LocalDate.of(2026, 3, 1))
                    .resultat(AGE_MINIMUM).satisfaite()).isTrue();
        }

        @Test
        void date_de_naissance_dans_le_futur() {
            assertThat(evaluer(neLe(AUJOURD_HUI.plusDays(1))).resultat(AGE_MINIMUM).satisfaite()).isFalse();
        }

        @Test
        void l_age_minimum_vient_du_parametre() {
            ReglesEligibilite vingtEtUn = new ReglesEligibilite(21, Fraction.de("1/3"));
            DossierEligibilite vingtAns = neLe(LocalDate.of(2006, 1, 1));
            assertThat(ConditionsEliminatoires.evaluer(vingtAns, vingtEtUn, AUJOURD_HUI)
                    .resultat(AGE_MINIMUM).satisfaite()).isFalse();
        }
    }

    @Nested
    class ContratEnCours {

        @Test
        void cdi_sans_date_de_fin() {
            assertThat(evaluer(contrat(LocalDate.of(2020, 1, 1), Optional.empty()))
                    .resultat(CONTRAT_EN_COURS).satisfaite()).isTrue();
        }

        @Test
        void commence_aujourd_hui() {
            assertThat(evaluer(contrat(AUJOURD_HUI, Optional.empty()))
                    .resultat(CONTRAT_EN_COURS).satisfaite()).isTrue();
        }

        @Test
        void pas_encore_commence() {
            assertThat(evaluer(contrat(AUJOURD_HUI.plusDays(1), Optional.empty()))
                    .resultat(CONTRAT_EN_COURS).satisfaite()).isFalse();
        }

        @Test
        void cdd_qui_finit_demain() {
            assertThat(evaluer(contrat(LocalDate.of(2025, 1, 1), Optional.of(AUJOURD_HUI.plusDays(1))))
                    .resultat(CONTRAT_EN_COURS).satisfaite()).isTrue();
        }

        @Test
        void cdd_qui_finit_aujourd_hui() {
            assertThat(evaluer(contrat(LocalDate.of(2025, 1, 1), Optional.of(AUJOURD_HUI)))
                    .resultat(CONTRAT_EN_COURS).satisfaite()).isFalse();
        }

        @Test
        void cdd_termine() {
            ResultatCondition r = evaluer(contrat(LocalDate.of(2025, 1, 1), Optional.of(LocalDate.of(2026, 6, 30))))
                    .resultat(CONTRAT_EN_COURS);
            assertThat(r.satisfaite()).isFalse();
            assertThat(r.raison()).contains("2026-06-30");
        }
    }

    @Nested
    class SalaireSurCompte {

        @Test
        void salaire_verse_sur_un_compte() {
            assertThat(evaluer(salaireSurCompte(true)).resultat(SALAIRE_SUR_COMPTE).satisfaite()).isTrue();
        }

        @Test
        void salaire_en_especes() {
            Eligibilite e = evaluer(salaireSurCompte(false));
            assertThat(e.estEligible()).isFalse();
            assertThat(e.nonSatisfaites()).extracting(ResultatCondition::condition).containsExactly(SALAIRE_SUR_COMPTE);
        }
    }

    @Nested
    class Endettement {

        @Test
        void exactement_le_tiers() {
            assertThat(evaluer(endettement(6_240, 0, 2_080)).resultat(ENDETTEMENT).satisfaite()).isTrue();
        }

        @Test
        void une_ouguiya_de_trop() {
            assertThat(evaluer(endettement(6_240, 0, 2_081)).resultat(ENDETTEMENT).satisfaite()).isFalse();
        }

        @Test
        void le_tiers_exact_et_non_33_pct() {
            // 33 % de 10 000 = 3 300, le tiers = 3 333,33 : 3 333 passe, 3 334 non
            assertThat(evaluer(endettement(10_000, 0, 3_333)).resultat(ENDETTEMENT).satisfaite()).isTrue();
            assertThat(evaluer(endettement(10_000, 0, 3_334)).resultat(ENDETTEMENT).satisfaite()).isFalse();
        }

        @Test
        void les_mensualites_en_cours_sont_ajoutees() {
            ResultatCondition r = evaluer(endettement(6_240, 1, 2_080)).resultat(ENDETTEMENT);
            assertThat(r.satisfaite()).isFalse();
            assertThat(r.raison()).contains("2081");
        }

        @Test
        void salaire_nul() {
            assertThat(evaluer(endettement(0, 0, 0)).resultat(ENDETTEMENT).satisfaite()).isFalse();
        }

        @Test
        void le_plafond_vient_du_parametre() {
            ReglesEligibilite quart = new ReglesEligibilite(18, Fraction.de("1/4"));
            assertThat(ConditionsEliminatoires.evaluer(endettement(8_320, 0, 2_080), quart, AUJOURD_HUI)
                    .resultat(ENDETTEMENT).satisfaite()).isTrue();
            assertThat(ConditionsEliminatoires.evaluer(endettement(8_319, 0, 2_080), quart, AUJOURD_HUI)
                    .resultat(ENDETTEMENT).satisfaite()).isFalse();
        }
    }

    @Nested
    class Retard {

        @Test
        void aucun_retard() {
            assertThat(evaluer(retard(false)).resultat(AUCUN_RETARD).satisfaite()).isTrue();
        }

        @Test
        void retard_en_cours() {
            Eligibilite e = evaluer(retard(true));
            assertThat(e.estEligible()).isFalse();
            assertThat(e.nonSatisfaites()).extracting(ResultatCondition::condition).containsExactly(AUCUN_RETARD);
        }
    }
}
