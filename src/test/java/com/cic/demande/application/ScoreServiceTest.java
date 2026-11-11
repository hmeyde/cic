package com.cic.demande.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Fraction;
import com.cic.commun.domain.Montant;
import com.cic.demande.domain.CritereScore;
import com.cic.demande.domain.DossierEligibilite;
import com.cic.demande.domain.DossierScore;
import com.cic.demande.domain.ModeRemboursement;
import com.cic.demande.domain.Recommandation;
import com.cic.demande.domain.Score;
import com.cic.demande.domain.TypeContrat;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;
import com.cic.parametrage.domain.Parametre;

class ScoreServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 11);

    private static final DossierEligibilite ELIGIBLE = new DossierEligibilite(true, true, LocalDate.of(1990, 5, 1),
            LocalDate.of(2025, 6, 1), Optional.empty(), true, Montant.de(30_000), Montant.ZERO, Montant.de(2_080),
            false);

    /** CDI depuis juin 2025 (10), virement permanent (8), 6,9 % (15), apport minimum : 58. */
    private static final DossierScore DOSSIER = new DossierScore(TypeContrat.CDI, LocalDate.of(2025, 6, 1),
            Optional.empty(), LocalDate.of(2027, 5, 25), ModeRemboursement.VIREMENT_PERMANENT,
            Montant.de(30_000), Montant.ZERO, Montant.de(2_080),
            Montant.de(20_800), Montant.de(8_320), Montant.de(8_320), false, 0);

    private static ScoreService service(List<Parametre> lignes) {
        ParametreService parametres = new ParametreService(() -> lignes);
        parametres.afterSingletonsInstantiated();
        Clock clock = Clock.fixed(AUJOURD_HUI.atStartOfDay(NOUAKCHOTT).toInstant(), NOUAKCHOTT);
        return new ScoreService(parametres, new EligibiliteService(parametres, clock), clock);
    }

    @Test
    void regles_lues_dans_les_parametres_de_depart() {
        var regles = service(ParametresDeDepart.lignes()).regles();

        assertThat(regles.versionGrille()).isEqualTo("v1");
        assertThat(regles.seuilFavorable()).isEqualTo(70);
        assertThat(regles.seuilAExaminer()).isEqualTo(50);
        assertThat(regles.endettementMax()).isEqualTo(Fraction.de("1/3"));
    }

    @Test
    void score_complet_avec_la_date_de_l_horloge() {
        Score score = service(ParametresDeDepart.lignes()).evaluer(ELIGIBLE, DOSSIER);

        assertThat(score.eligibilite().estEligible()).isTrue();
        assertThat(score.versionGrille()).isEqualTo("v1");
        assertThat(score.note().resultat(CritereScore.ANCIENNETE).points()).isEqualTo(10);
        assertThat(score.note().note()).isEqualTo(58);
        assertThat(score.recommandation()).isEqualTo(Recommandation.A_EXAMINER);
    }

    @Test
    void seuil_modifie_en_parametre() {
        Score score = service(ParametresDeDepart.avec(CleParametre.SCORE_SEUIL_A_EXAMINER, "60"))
                .evaluer(ELIGIBLE, DOSSIER);

        assertThat(score.recommandation()).isEqualTo(Recommandation.DEFAVORABLE);
    }

    @Test
    void dossier_ineligible_defavorable() {
        DossierEligibilite enRetard = new DossierEligibilite(true, true, LocalDate.of(1990, 5, 1),
                LocalDate.of(2025, 6, 1), Optional.empty(), true, Montant.de(30_000), Montant.ZERO,
                Montant.de(2_080), true);

        Score score = service(ParametresDeDepart.lignes()).evaluer(enRetard, DOSSIER);

        assertThat(score.note().recommandation()).isEqualTo(Recommandation.A_EXAMINER);
        assertThat(score.recommandation()).isEqualTo(Recommandation.DEFAVORABLE);
    }

    @Test
    void version_de_grille_inconnue_refusee() {
        ScoreService s = service(ParametresDeDepart.avec(CleParametre.SCORE_GRILLE_VERSION, "v2"));

        assertThatThrownBy(() -> s.noter(DOSSIER)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("v2");
    }
}
