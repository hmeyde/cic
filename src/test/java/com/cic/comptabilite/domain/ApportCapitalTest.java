package com.cic.comptabilite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;

class ApportCapitalTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 12);
    private static final Instant LE = Instant.parse("2026-11-12T10:00:00Z");

    @Test
    void un_apport_garde_l_associe_sans_espaces_superflus_le_montant_et_la_date() {
        ApportCapital a = ApportCapital.saisir("  Sidi   Mohamed ", Montant.de(500_000), AUJOURDHUI, AUJOURDHUI,
                "admin", LE);

        assertThat(a.associe()).isEqualTo("Sidi Mohamed");
        assertThat(a.montant()).isEqualTo(Montant.de(500_000));
        assertThat(a.verseLe()).isEqualTo(AUJOURDHUI);
        assertThat(a.saisiPar()).isEqualTo("admin");
        assertThat(a.toString()).doesNotContain("Sidi");
    }

    @Test
    void un_apport_sans_associe_de_montant_nul_ou_date_dans_le_futur_est_refuse() {
        assertThatThrownBy(() -> ApportCapital.saisir(" ", Montant.de(1), AUJOURDHUI, AUJOURDHUI, "admin", LE))
                .isInstanceOfSatisfying(ApportCapitalInvalideException.class,
                        e -> assertThat(e.code()).isEqualTo(ApportCapitalInvalideException.ASSOCIE_INVALIDE));
        assertThatThrownBy(() -> ApportCapital.saisir("x".repeat(101), Montant.de(1), AUJOURDHUI, AUJOURDHUI,
                "admin", LE)).isInstanceOf(ApportCapitalInvalideException.class);
        assertThatThrownBy(() -> ApportCapital.saisir("Sidi", Montant.ZERO, AUJOURDHUI, AUJOURDHUI, "admin", LE))
                .isInstanceOfSatisfying(ApportCapitalInvalideException.class,
                        e -> assertThat(e.code()).isEqualTo(ApportCapitalInvalideException.MONTANT_INVALIDE));
        assertThatThrownBy(() -> ApportCapital.saisir("Sidi", Montant.de(1), AUJOURDHUI.plusDays(1), AUJOURDHUI,
                "admin", LE)).isInstanceOfSatisfying(ApportCapitalInvalideException.class,
                        e -> assertThat(e.code()).isEqualTo(ApportCapitalInvalideException.DATE_INVALIDE));
    }

    @Test
    void l_ecriture_d_un_apport_en_capital_debite_la_banque_et_credite_le_capital() {
        ApportCapital a = ApportCapital.saisir("Sidi", Montant.de(500_000), AUJOURDHUI, AUJOURDHUI, "admin", LE);

        EcritureComptable e = EcritureComptable.apportCapital(a.id(), a.montant(), a.verseLe(), LE);

        assertThat(e.nature()).isEqualTo(NatureEcriture.APPORT_CAPITAL);
        assertThat(e.sourceId()).isEqualTo(a.id());
        assertThat(e.demandeId()).isEmpty();
        assertThat(e.libelle()).doesNotContain("Sidi");
        assertThat(e.lignes()).containsExactly(LigneEcriture.debit(Compte.BANQUE, Montant.de(500_000)),
                LigneEcriture.credit(Compte.CAPITAL, Montant.de(500_000)));
    }
}
