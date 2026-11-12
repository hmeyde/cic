package com.cic.contrat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;

class EcheanceTest {

    private final Echeance aVenir = Echeance.reconstituer(UUID.randomUUID(), 1, LocalDate.of(2026, 11, 25),
            Montant.de(2_080), StatutEcheance.A_VENIR);

    @Test
    void une_echeance_impayee_passe_en_retard_sans_changer_de_montant() {
        Echeance enRetard = aVenir.enRetard();

        assertThat(enRetard.estEnRetard()).isTrue();
        assertThat(enRetard.montant()).isEqualTo(Montant.de(2_080));
        assertThat(enRetard.payee().estPayee()).isTrue();
    }

    @Test
    void seule_une_echeance_a_venir_passe_en_retard() {
        assertThatThrownBy(() -> aVenir.enRetard().enRetard()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> aVenir.payee().enRetard()).isInstanceOf(IllegalStateException.class);
    }
}
