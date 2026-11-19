package com.seheli.pilotage.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;

/** Indicateurs de risque et alertes sur les seuils de départ de CLAUDE.md. */
class IndicateursRisqueTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 12);
    private static final SeuilsRisque SEUILS = new SeuilsRisque(30, new BigDecimal("50"), 90, new BigDecimal("100"),
            180, new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("90"), new BigDecimal("90"));

    private static PositionDossier dossier(long restantDu, String employeur, String fournisseur,
            Integer joursDeRetard) {
        return new PositionDossier(UUID.randomUUID(), Montant.de(restantDu), Optional.ofNullable(employeur),
                fournisseur, Optional.ofNullable(joursDeRetard).map(j -> AUJOURDHUI.minusDays(j)));
    }

    /** Cinq employeurs et cinq fournisseurs à 20 % chacun, aucun retard, prélèvements tous payés. */
    private static List<PositionDossier> portefeuilleSain() {
        return List.of(dossier(2_000, "SNIM", "F1", null), dossier(2_000, "Mauritel", "F2", null),
                dossier(2_000, "Ministère de la Santé", "F3", null), dossier(2_000, "BMCI", "F4", null),
                dossier(2_000, "SOMELEC", "F5", null));
    }

    @Test
    void un_portefeuille_dans_les_seuils_ne_leve_aucune_alerte() {
        IndicateursRisque r = IndicateursRisque.calculer(portefeuilleSain(), 12, 0, Montant.de(100_000), SEUILS,
                AUJOURDHUI);

        assertThat(r.alertes()).isEmpty();
        assertThat(r.aDesAlertes()).isFalse();
        assertThat(r.tauxPrelevementsReussisPct()).contains(new BigDecimal("100.0"));
        assertThat(r.utilisationCapitalPct()).contains(new BigDecimal("10.0"));
        assertThat(r.employeurPrincipal().orElseThrow().partPct()).isEqualByComparingTo("20");
        assertThat(r.fournisseurPrincipal().orElseThrow().partPct()).isEqualByComparingTo("20");
    }

    @Test
    void sans_encours_ni_echeance_echue_les_indicateurs_sont_vides_et_sans_alerte() {
        IndicateursRisque r = IndicateursRisque.calculer(List.of(), 0, 0, Montant.ZERO, SEUILS, AUJOURDHUI);

        assertThat(r.alertes()).isEmpty();
        assertThat(r.tauxPrelevementsReussisPct()).isEmpty();
        assertThat(r.utilisationCapitalPct()).isEmpty();
        assertThat(r.employeurPrincipal()).isEmpty();
        assertThat(r.fournisseurPrincipal()).isEmpty();
    }

    @Test
    void un_retard_de_30_jours_est_provisionne_a_50_pct_puis_100_pct_a_90_jours() {
        List<PositionDossier> positions = List.of(dossier(12_480, "A", "F1", 29), dossier(10_000, "B", "F2", 30),
                dossier(4_160, "C", "F3", 90), dossier(1_000, "D", "F4", null), dossier(1_000, "E", "F5", null));

        IndicateursRisque r = IndicateursRisque.calculer(positions, 10, 3, Montant.de(1_000_000), SEUILS,
                AUJOURDHUI);

        assertThat(r.dossiersRetard30()).isEqualTo(2);
        assertThat(r.restantDuRetard30()).isEqualTo(Montant.de(14_160));
        assertThat(r.provisions()).isEqualTo(Montant.de(5_000 + 4_160));
        assertThat(r.dossiersPerte()).isZero();
        assertThat(r.alertes()).contains(TypeAlerte.RETARD_30_JOURS).doesNotContain(TypeAlerte.PERTE);
    }

    @Test
    void a_180_jours_de_retard_le_restant_du_passe_en_perte_et_n_est_plus_provisionne() {
        IndicateursRisque r = IndicateursRisque.calculer(List.of(dossier(4_160, "A", "F1", 180)), 5, 1,
                Montant.de(1_000_000), SEUILS, AUJOURDHUI);

        assertThat(r.dossiersPerte()).isEqualTo(1);
        assertThat(r.perte()).isEqualTo(Montant.de(4_160));
        assertThat(r.dossiersRetard30()).isZero();
        assertThat(r.provisions()).isEqualTo(Montant.ZERO);
        assertThat(r.enAlerte("PERTE")).isTrue();
        assertThat(r.enAlerte("RETARD_30_JOURS")).isFalse();
    }

    @Test
    void des_prelevements_reussis_sous_90_pct_levent_une_alerte() {
        IndicateursRisque juste = IndicateursRisque.calculer(portefeuilleSain(), 9, 1, Montant.de(100_000), SEUILS,
                AUJOURDHUI);
        IndicateursRisque sous = IndicateursRisque.calculer(portefeuilleSain(), 8, 1, Montant.de(100_000), SEUILS,
                AUJOURDHUI);

        assertThat(juste.tauxPrelevementsReussisPct()).contains(new BigDecimal("90.0"));
        assertThat(juste.alertes()).isEmpty();
        assertThat(sous.tauxPrelevementsReussisPct()).contains(new BigDecimal("88.9"));
        assertThat(sous.alertes()).containsExactly(TypeAlerte.PRELEVEMENTS_REUSSIS);
    }

    @Test
    void un_encours_au_dela_de_90_pct_du_capital_disponible_leve_une_alerte() {
        IndicateursRisque juste = IndicateursRisque.calculer(portefeuilleSain(), 0, 0, Montant.de(11_112), SEUILS,
                AUJOURDHUI);
        IndicateursRisque au = IndicateursRisque.calculer(portefeuilleSain(), 0, 0, Montant.de(11_100), SEUILS,
                AUJOURDHUI);
        IndicateursRisque sansCapital = IndicateursRisque.calculer(portefeuilleSain(), 0, 0, Montant.ZERO, SEUILS,
                AUJOURDHUI);

        assertThat(juste.utilisationCapitalPct()).contains(new BigDecimal("90.0"));
        assertThat(juste.alertes()).isEmpty();
        assertThat(au.utilisationCapitalPct()).contains(new BigDecimal("90.1"));
        assertThat(au.alertes()).containsExactly(TypeAlerte.UTILISATION_CAPITAL);
        assertThat(sansCapital.utilisationCapitalPct()).isEmpty();
        assertThat(sansCapital.alertes()).containsExactly(TypeAlerte.UTILISATION_CAPITAL);
    }

    @Test
    void la_concentration_regroupe_un_employeur_ecrit_differemment_et_alerte_au_dela_de_20_pct() {
        List<PositionDossier> positions = List.of(dossier(3_000, "SNIM", "F1", null),
                dossier(2_000, " snim ", "F2", null), dossier(2_500, "Mauritel", "F3", null),
                dossier(2_500, null, "F3", null));

        IndicateursRisque r = IndicateursRisque.calculer(positions, 0, 0, Montant.de(1_000_000), SEUILS,
                AUJOURDHUI);

        Concentration employeur = r.employeurPrincipal().orElseThrow();
        assertThat(employeur.libelle()).contains("SNIM");
        assertThat(employeur.montant()).isEqualTo(Montant.de(5_000));
        assertThat(employeur.partPct()).isEqualByComparingTo("50.0");
        Concentration fournisseur = r.fournisseurPrincipal().orElseThrow();
        assertThat(fournisseur.libelle()).contains("F3");
        assertThat(fournisseur.partPct()).isEqualByComparingTo("50.0");
        assertThat(r.alertes()).containsExactly(TypeAlerte.CONCENTRATION_EMPLOYEUR,
                TypeAlerte.CONCENTRATION_FOURNISSEUR);
    }

    @Test
    void la_repartition_classe_les_groupes_du_plus_gros_au_plus_petit_a_egalite_dans_l_ordre_de_rencontre() {
        List<PositionDossier> positions = List.of(dossier(3_000, "SNIM", "F1", null),
                dossier(2_000, " snim ", "F2", null), dossier(2_500, "Mauritel", "F3", null),
                dossier(2_500, null, "F3", null));

        IndicateursRisque r = IndicateursRisque.calculer(positions, 0, 0, Montant.de(1_000_000), SEUILS,
                AUJOURDHUI);

        assertThat(r.repartitionEmployeurs()).extracting(Concentration::libelle)
                .containsExactly(Optional.of("SNIM"), Optional.of("Mauritel"), Optional.empty());
        assertThat(r.repartitionEmployeurs()).extracting(Concentration::partPct)
                .containsExactly(new BigDecimal("50.0"), new BigDecimal("25.0"), new BigDecimal("25.0"));
        assertThat(r.repartitionFournisseurs()).extracting(c -> c.libelle().orElseThrow())
                .containsExactly("F3", "F1", "F2");
        assertThat(r.aUneAlerteCritique()).isFalse();
    }

    @Test
    void la_perte_et_le_depassement_du_capital_sont_des_alertes_critiques() {
        IndicateursRisque r = IndicateursRisque.calculer(List.of(dossier(10_000, "SNIM", "F1", 200)), 0, 0,
                Montant.de(1_000), SEUILS, AUJOURDHUI);

        assertThat(r.alertes()).contains(TypeAlerte.PERTE, TypeAlerte.UTILISATION_CAPITAL);
        assertThat(r.aUneAlerteCritique()).isTrue();
    }
}
