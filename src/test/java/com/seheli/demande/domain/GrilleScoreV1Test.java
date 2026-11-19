package com.seheli.demande.domain;

import static com.seheli.demande.domain.CritereScore.ANCIENNETE;
import static com.seheli.demande.domain.CritereScore.APPORT_SUPPLEMENTAIRE;
import static com.seheli.demande.domain.CritereScore.GARANT;
import static com.seheli.demande.domain.CritereScore.HISTORIQUE_SEHELI;
import static com.seheli.demande.domain.CritereScore.MODE_REMBOURSEMENT;
import static com.seheli.demande.domain.CritereScore.PART_SALAIRE;
import static com.seheli.demande.domain.CritereScore.TYPE_CONTRAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.seheli.commun.domain.Fraction;
import com.seheli.commun.domain.Montant;

class GrilleScoreV1Test {

    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 11);
    private static final ReglesScore REGLES = new ReglesScore("v1", 70, 50, Fraction.de("1/3"));

    /**
     * Exemple chiffré : prix de vente 20 800, apport minimum 8 320, 6 × 2 080.
     * CDI depuis 2020, retenue employeur, salaire 30 000 (6,9 %), sans garant ni historique : 70.
     */
    private static final DossierScore BASE = new DossierScore(
            TypeContrat.CDI, LocalDate.of(2020, 1, 1), Optional.empty(), LocalDate.of(2027, 5, 25),
            ModeRemboursement.RETENUE_EMPLOYEUR,
            Montant.de(30_000), Montant.ZERO, Montant.de(2_080),
            Montant.de(20_800), Montant.de(8_320), Montant.de(8_320),
            false, 0);

    private static Builder dossier() {
        return new Builder();
    }

    private static NoteScore noter(DossierScore d) {
        return GrilleScoreV1.noter(d, REGLES, AUJOURD_HUI);
    }

    @Test
    void les_maximums_de_la_grille_font_100() {
        assertThat(Arrays.stream(CritereScore.values()).mapToInt(CritereScore::pointsMax).sum()).isEqualTo(100);
    }

    @Test
    void note_de_l_exemple_chiffre_avec_version_et_criteres_dans_l_ordre() {
        NoteScore note = noter(BASE);

        assertThat(note.versionGrille()).isEqualTo("v1");
        assertThat(note.criteres()).extracting(ResultatCritere::critere).containsExactly(
                TYPE_CONTRAT, ANCIENNETE, MODE_REMBOURSEMENT, PART_SALAIRE, APPORT_SUPPLEMENTAIRE, GARANT,
                HISTORIQUE_SEHELI);
        assertThat(note.criteres()).extracting(ResultatCritere::points).containsExactly(25, 15, 15, 15, 0, 0, 0);
        assertThat(note.criteres()).allSatisfy(r -> assertThat(r.raison()).isNotBlank());
        assertThat(note.note()).isEqualTo(70);
        assertThat(note.recommandation()).isEqualTo(Recommandation.FAVORABLE);
    }

    @Test
    void note_maximale_100() {
        NoteScore note = noter(dossier().apport(20_800).garant(true).plans(2).build());

        assertThat(note.note()).isEqualTo(100);
    }

    @Nested
    class TypeContratCritere {

        @Test
        void fonctionnaire_25() {
            ResultatCritere r = GrilleScoreV1.typeContrat(dossier().type(TypeContrat.FONCTIONNAIRE).build());
            assertThat(r.points()).isEqualTo(25);
            assertThat(r.raison()).contains("Fonctionnaire");
        }

        @Test
        void cdi_25() {
            assertThat(GrilleScoreV1.typeContrat(BASE).points()).isEqualTo(25);
        }

        @Test
        void cdd_finissant_apres_la_derniere_echeance_15() {
            ResultatCritere r = GrilleScoreV1.typeContrat(dossier().cdd(LocalDate.of(2027, 5, 26)).build());
            assertThat(r.points()).isEqualTo(15);
            assertThat(r.raison()).contains("CDD", "2027-05-26");
        }

        @Test
        void cdd_finissant_le_jour_de_la_derniere_echeance_0() {
            assertThat(GrilleScoreV1.typeContrat(dossier().cdd(LocalDate.of(2027, 5, 25)).build()).points())
                    .isZero();
        }

        @Test
        void cdd_sans_date_de_fin_refuse() {
            assertThatThrownBy(() -> dossier().type(TypeContrat.CDD).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class AncienneteCritere {

        @ParameterizedTest(name = "entré le {0} : {1} points")
        @CsvSource({
                "2024-11-10, 15",  // 2 ans et 1 jour
                "2024-11-11, 10",  // 2 ans tout juste
                "2025-11-11, 10",  // 1 an tout juste
                "2025-11-12, 5",   // 1 an moins 1 jour
                "2026-11-11, 5",   // premier jour
                "2026-11-12, 0"    // pas encore commencé
        })
        void bareme_et_limites(LocalDate debut, int points) {
            ResultatCritere r = GrilleScoreV1.anciennete(dossier().debut(debut).build(), AUJOURD_HUI);
            assertThat(r.points()).isEqualTo(points);
            assertThat(r.raison()).contains(debut.toString());
        }
    }

    @Nested
    class ModeRemboursementCritere {

        @ParameterizedTest(name = "{0} : {1} points")
        @CsvSource({"RETENUE_EMPLOYEUR, 15", "DOMICILIATION_BMCI, 15", "VIREMENT_PERMANENT, 8"})
        void bareme(ModeRemboursement mode, int points) {
            ResultatCritere r = GrilleScoreV1.modeRemboursement(dossier().mode(mode).build());
            assertThat(r.points()).isEqualTo(points);
            assertThat(r.raison()).isNotBlank();
        }
    }

    @Nested
    class PartSalaireCritere {

        @ParameterizedTest(name = "salaire {0}, en cours {1}, nouvelle {2} : {3} points")
        @CsvSource({
                "30000,    0, 2080, 15",   // 6,9 %
                "10000,    0, 1999, 15",   // 19,99 %
                "10000,    0, 2000, 8",    // 20 % tout juste
                "10000, 1000, 1000, 8",    // mensualités en cours comprises
                "30000,    0, 10000, 8",   // 1/3 exact
                "30001,    0, 10000, 8",
                "29999,    0, 10000, 0",   // au-delà de 1/3
                "0,        0, 2080, 0"     // salaire nul
        })
        void bareme_et_limites(long salaire, long enCours, long nouvelle, int points) {
            ResultatCritere r = GrilleScoreV1.partSalaire(
                    dossier().salaire(salaire, enCours, nouvelle).build(), REGLES);
            assertThat(r.points()).isEqualTo(points);
            assertThat(r.raison()).isNotBlank();
        }

        @Test
        void raison_avec_le_pourcentage() {
            assertThat(GrilleScoreV1.partSalaire(BASE, REGLES).raison()).contains("6.9 %");
        }

        @Test
        void plafond_lu_dans_les_regles() {
            ReglesScore quart = new ReglesScore("v1", 70, 50, Fraction.de("1/4"));
            assertThat(GrilleScoreV1.partSalaire(dossier().salaire(10_000, 0, 2_600).build(), quart).points())
                    .isZero();
        }
    }

    @Nested
    class ApportCritere {

        @ParameterizedTest(name = "apport {0} : {1} points")
        @CsvSource({
                "8320, 0",     // minimum
                "9567, 0",     // 1 247 / 12 480 → 0,99
                "9568, 1",     // 1 248 / 12 480 → 1
                "14560, 5",    // moitié de la plage
                "20799, 9",
                "20800, 10",   // prix de vente
                "8000, 0"      // inférieur au minimum
        })
        void proportionnel_a_l_apport_supplementaire(long apport, int points) {
            ResultatCritere r = GrilleScoreV1.apportSupplementaire(dossier().apport(apport).build());
            assertThat(r.points()).isEqualTo(points);
            assertThat(r.raison()).isNotBlank();
        }

        @Test
        void raison_avec_le_supplement() {
            assertThat(GrilleScoreV1.apportSupplementaire(dossier().apport(14_560).build()).raison())
                    .contains("6240 MRU", "8320 MRU");
        }
    }

    @Nested
    class GarantCritere {

        @Test
        void avec_garant_10() {
            assertThat(GrilleScoreV1.garant(dossier().garant(true).build()).points()).isEqualTo(10);
        }

        @Test
        void sans_garant_0() {
            ResultatCritere r = GrilleScoreV1.garant(BASE);
            assertThat(r.points()).isZero();
            assertThat(r.raison()).isEqualTo("Aucun garant");
        }
    }

    @Nested
    class HistoriqueCritere {

        @ParameterizedTest(name = "{0} plan(s) : {1} points")
        @CsvSource({"0, 0", "1, 5", "2, 10", "5, 10"})
        void cinq_points_par_plan_plafonne_a_10(int plans, int points) {
            ResultatCritere r = GrilleScoreV1.historique(dossier().plans(plans).build());
            assertThat(r.points()).isEqualTo(points);
            assertThat(r.raison()).isNotBlank();
        }

        @Test
        void nombre_negatif_refuse() {
            assertThatThrownBy(() -> dossier().plans(-1).build()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Lecture {

        @ParameterizedTest(name = "note {0} : {1}")
        @CsvSource({"100, FAVORABLE", "70, FAVORABLE", "69, A_EXAMINER", "50, A_EXAMINER", "49, DEFAVORABLE",
                "0, DEFAVORABLE"})
        void seuils(int note, Recommandation attendue) {
            assertThat(Recommandation.selon(note, REGLES)).isEqualTo(attendue);
        }

        @Test
        void seuils_lus_dans_les_regles() {
            assertThat(Recommandation.selon(70, new ReglesScore("v1", 80, 60, Fraction.de("1/3"))))
                    .isEqualTo(Recommandation.A_EXAMINER);
        }

        @Test
        void seuils_incoherents_refuses() {
            assertThatThrownBy(() -> new ReglesScore("v1", 50, 70, Fraction.de("1/3")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void dossier_a_examiner() {
            // CDD 15 + moins d'un an 5 + virement 8 + 20 à 33 % 8 + garant 10 = 46 → DEFAVORABLE ; + 1 plan = 51
            DossierScore d = dossier().cdd(LocalDate.of(2028, 1, 1)).debut(LocalDate.of(2026, 3, 1))
                    .mode(ModeRemboursement.VIREMENT_PERMANENT).salaire(8_000, 0, 2_080).garant(true).build();
            assertThat(noter(d).note()).isEqualTo(46);
            assertThat(noter(d).recommandation()).isEqualTo(Recommandation.DEFAVORABLE);

            DossierScore avecHistorique = new Builder(d).plans(1).build();
            assertThat(noter(avecHistorique).note()).isEqualTo(51);
            assertThat(noter(avecHistorique).recommandation()).isEqualTo(Recommandation.A_EXAMINER);
        }
    }

    @Test
    void points_hors_bareme_refuses() {
        assertThatThrownBy(() -> new ResultatCritere(GARANT, 11, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultatCritere(GARANT, -1, "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    class ScoreComplet {

        private final Eligibilite eligible = new Eligibilite(java.util.List.of(
                new ResultatCondition(ConditionEliminatoire.AGE_MINIMUM, true, "ok")));
        private final Eligibilite ineligible = new Eligibilite(java.util.List.of(
                new ResultatCondition(ConditionEliminatoire.AUCUN_RETARD, false, "retard")));

        @Test
        void eligible_suit_la_note() {
            Score s = new Score(eligible, noter(BASE));
            assertThat(s.recommandation()).isEqualTo(Recommandation.FAVORABLE);
            assertThat(s.versionGrille()).isEqualTo("v1");
        }

        @Test
        void ineligible_defavorable_quelle_que_soit_la_note() {
            Score s = new Score(ineligible, noter(dossier().apport(20_800).garant(true).plans(2).build()));
            assertThat(s.note().note()).isEqualTo(100);
            assertThat(s.recommandation()).isEqualTo(Recommandation.DEFAVORABLE);
        }
    }

    private static final class Builder {
        private TypeContrat type;
        private LocalDate debut;
        private Optional<LocalDate> fin;
        private final LocalDate derniereEcheance;
        private ModeRemboursement mode;
        private Montant salaire;
        private Montant enCours;
        private Montant nouvelle;
        private final Montant prixVente;
        private final Montant apportMinimum;
        private Montant apport;
        private boolean garant;
        private int plans;

        Builder() {
            this(BASE);
        }

        Builder(DossierScore d) {
            type = d.typeContrat();
            debut = d.debutContrat();
            fin = d.finContrat();
            derniereEcheance = d.derniereEcheance();
            mode = d.modeRemboursement();
            salaire = d.salaireNet();
            enCours = d.mensualitesSeheliEnCours();
            nouvelle = d.nouvelleMensualite();
            prixVente = d.prixVente();
            apportMinimum = d.apportMinimum();
            apport = d.apport();
            garant = d.garant();
            plans = d.plansRembourseATemps();
        }

        Builder type(TypeContrat t) { type = t; return this; }
        Builder cdd(LocalDate f) { type = TypeContrat.CDD; fin = Optional.of(f); return this; }
        Builder debut(LocalDate d) { debut = d; return this; }
        Builder mode(ModeRemboursement m) { mode = m; return this; }
        Builder salaire(long s, long c, long n) {
            salaire = Montant.de(s); enCours = Montant.de(c); nouvelle = Montant.de(n); return this;
        }
        Builder apport(long a) { apport = Montant.de(a); return this; }
        Builder garant(boolean g) { garant = g; return this; }
        Builder plans(int p) { plans = p; return this; }

        DossierScore build() {
            return new DossierScore(type, debut, fin, derniereEcheance, mode, salaire, enCours, nouvelle,
                    prixVente, apportMinimum, apport, garant, plans);
        }
    }
}
