package com.cic.inscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class InscriptionTest {

    private static final Instant LE = Instant.parse("2026-11-12T10:00:00Z");

    @Test
    void une_inscription_complete_est_normalisee_et_horodate_le_consentement() {
        Inscription i = Inscription.nouvelle("  Mariem   Sidi ", "+222 22 12 34 56", " SNIM ", TypeContrat.CDI,
                TrancheSalaire.DE_10000_A_20000, "Smartphone", true, LE);

        assertThat(i.nom()).isEqualTo("Mariem Sidi");
        assertThat(i.telephone()).isEqualTo("22123456");
        assertThat(i.employeur()).isEqualTo("SNIM");
        assertThat(i.consentementLe()).isEqualTo(LE);
        assertThat(i.toString()).doesNotContain("Mariem", "22123456", "SNIM");
    }

    @Test
    void le_consentement_est_obligatoire() {
        assertThatThrownBy(() -> Inscription.nouvelle("Mariem", "22123456", "SNIM", TypeContrat.CDI,
                TrancheSalaire.DE_10000_A_20000, "Smartphone", false, LE))
                .isInstanceOfSatisfying(InscriptionInvalideException.class,
                        e -> assertThat(e.code()).isEqualTo(InscriptionInvalideException.CONSENTEMENT_MANQUANT));
    }

    @Test
    void un_telephone_mal_forme_est_refuse() {
        assertThatThrownBy(() -> Inscription.normaliserTelephone("1234"))
                .isInstanceOfSatisfying(InscriptionInvalideException.class,
                        e -> assertThat(e.champ()).isEqualTo("telephone"));
        assertThat(Inscription.normaliserTelephone("00222 36-12-34-56")).isEqualTo("36123456");
    }

    @Test
    void les_champs_obligatoires_sont_controles() {
        assertThatThrownBy(() -> Inscription.nouvelle(" ", "22123456", "SNIM", TypeContrat.CDI,
                TrancheSalaire.DE_10000_A_20000, "Smartphone", true, LE))
                .hasFieldOrPropertyWithValue("code", InscriptionInvalideException.NOM_INVALIDE);
        assertThatThrownBy(() -> Inscription.nouvelle("Mariem", "22123456", "SNIM", null,
                TrancheSalaire.DE_10000_A_20000, "Smartphone", true, LE))
                .hasFieldOrPropertyWithValue("code", InscriptionInvalideException.TYPE_CONTRAT_INVALIDE);
        assertThatThrownBy(() -> Inscription.nouvelle("Mariem", "22123456", "SNIM", TypeContrat.CDD, null,
                "Smartphone", true, LE))
                .hasFieldOrPropertyWithValue("code", InscriptionInvalideException.TRANCHE_SALAIRE_INVALIDE);
        assertThatThrownBy(() -> Inscription.nouvelle("Mariem", "22123456", "SNIM", TypeContrat.CDD,
                TrancheSalaire.PLUS_DE_40000, "x".repeat(151), true, LE))
                .hasFieldOrPropertyWithValue("code", InscriptionInvalideException.PRODUIT_INVALIDE);
    }
}
