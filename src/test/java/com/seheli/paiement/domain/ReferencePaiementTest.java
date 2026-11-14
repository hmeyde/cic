package com.seheli.paiement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReferencePaiementTest {

    @Test
    void la_saisie_est_normalisee() {
        assertThat(ReferencePaiement.lire("  ap 2026-000.0011 ").valeur()).isEqualTo("AP20260000011");
    }

    @Test
    void lettres_et_chiffres_seulement_trente_caracteres_au_plus() {
        assertThat(ReferencePaiement.lire("X".repeat(30)).valeur()).hasSize(30);
        assertThatThrownBy(() -> ReferencePaiement.lire("X".repeat(31))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReferencePaiement.lire("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReferencePaiement.lire("AP2026/1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReferencePaiement.lire("AP٢٠٢٦")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconnait_une_reference_d_apport_valide() {
        assertThat(ReferencePaiement.lire("AP20260000011").commeReferenceApport())
                .hasValueSatisfying(r -> assertThat(r.chiffresContrat()).isEqualTo("2026000001"));
        assertThat(ReferencePaiement.lire("AP20260000012").commeReferenceApport()).isEmpty();
        assertThat(ReferencePaiement.lire("TRX123").commeReferenceApport()).isEmpty();
    }
}
