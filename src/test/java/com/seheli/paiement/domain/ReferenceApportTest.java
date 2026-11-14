package com.seheli.paiement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReferenceApportTest {

    @Test
    void tiree_du_numero_de_contrat_avec_une_cle_de_luhn() {
        assertThat(ReferenceApport.pourContrat("SEH-2026-000001").valeur()).isEqualTo("AP20260000011");
        assertThat(ReferenceApport.pourContrat("SEH-2026-000042").valeur()).isEqualTo("AP20260000425");
    }

    @Test
    void deux_contrats_differents_donnent_deux_references_differentes() {
        assertThat(ReferenceApport.pourContrat("SEH-2026-000001"))
                .isNotEqualTo(ReferenceApport.pourContrat("SEH-2026-000002"))
                .isNotEqualTo(ReferenceApport.pourContrat("SEH-2027-000001"));
    }

    @Test
    void une_reference_generee_est_valide() {
        assertThat(ReferenceApport.estValide(ReferenceApport.pourContrat("SEH-2026-000123").valeur())).isTrue();
    }

    @Test
    void detecte_un_chiffre_errone_ou_deux_chiffres_inverses() {
        assertThat(ReferenceApport.estValide("AP20260000012")).isFalse();
        assertThat(ReferenceApport.estValide("AP20260000425")).isTrue();
        assertThat(ReferenceApport.estValide("AP20260004025")).isFalse();
    }

    @Test
    void refuse_une_saisie_mal_formee() {
        assertThat(ReferenceApport.estValide(null)).isFalse();
        assertThat(ReferenceApport.estValide("")).isFalse();
        assertThat(ReferenceApport.estValide("SEH-2026-000001")).isFalse();
        assertThat(ReferenceApport.estValide("ap20260000011")).isFalse();
        assertThatThrownBy(() -> new ReferenceApport("AP20260000012")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReferenceApport.pourContrat("CIC")).isInstanceOf(IllegalArgumentException.class);
    }
}
