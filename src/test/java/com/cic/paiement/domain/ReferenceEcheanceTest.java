package com.cic.paiement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReferenceEcheanceTest {

    @Test
    void tiree_du_numero_de_contrat_et_du_rang_avec_une_cle_de_luhn() {
        ReferenceEcheance r = ReferenceEcheance.pourEcheance("CIC-2026-000042", 3);

        assertThat(r.valeur()).startsWith("EC202600004203").hasSize(15);
        assertThat(r.chiffresContrat()).isEqualTo("2026000042");
        assertThat(r.rang()).isEqualTo(3);
        assertThat(ReferenceEcheance.estValide(r.valeur())).isTrue();
    }

    @Test
    void chaque_echeance_a_sa_reference_distincte_de_celle_de_l_apport() {
        assertThat(ReferenceEcheance.pourEcheance("CIC-2026-000001", 1))
                .isNotEqualTo(ReferenceEcheance.pourEcheance("CIC-2026-000001", 2));
        String echeance = ReferenceEcheance.pourEcheance("CIC-2026-000001", 1).valeur();
        assertThat(ReferenceApport.estValide(echeance)).isFalse();
        assertThat(ReferenceEcheance.estValide(ReferenceApport.pourContrat("CIC-2026-000001").valeur())).isFalse();
        assertThat(new ReferencePaiement(echeance).commeReferenceEcheance()).isPresent();
        assertThat(new ReferencePaiement(echeance).commeReferenceApport()).isEmpty();
    }

    @Test
    void exemple_de_l_aide_du_back_office() {
        assertThat(ReferenceEcheance.pourEcheance("CIC-2026-000001", 1).valeur()).isEqualTo("EC2026000001019");
    }

    @Test
    void detecte_une_faute_de_frappe_et_refuse_une_saisie_mal_formee() {
        String juste = ReferenceEcheance.pourEcheance("CIC-2026-000042", 3).valeur();
        String faute = juste.substring(0, 5) + (char) ('0' + (juste.charAt(5) - '0' + 1) % 10) + juste.substring(6);

        assertThat(ReferenceEcheance.estValide(faute)).isFalse();
        assertThat(ReferenceEcheance.estValide(null)).isFalse();
        assertThat(ReferenceEcheance.estValide("EC")).isFalse();
        assertThatThrownBy(() -> ReferenceEcheance.pourEcheance("CIC-2026-000042", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReferenceEcheance.pourEcheance("CIC", 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
