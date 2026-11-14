package com.cic.contrat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;

class ContratTest {

    private static final Prix PRIX = new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800),
            Montant.de(8_320));
    private static final Instant LE = Instant.parse("2026-11-13T09:30:00Z");

    private static Contrat etablir(Montant apport, Echeancier plan) {
        return Contrat.etablir("SEH-2026-000001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35", PRIX, apport, plan, LE);
    }

    private static Echeancier plan(Montant reste) {
        return Echeancier.planifier(reste, 6, LocalDate.of(2026, 11, 20), 25, Optional.empty());
    }

    @Test
    void apport_et_echeances_font_le_prix_de_vente() {
        Contrat c = etablir(Montant.de(8_320), plan(Montant.de(12_480)));

        assertThat(c.nombreMois()).isEqualTo(6);
        assertThat(c.echeances()).extracting(Echeance::rang).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(c.echeances()).allSatisfy(e -> assertThat(e.statut()).isEqualTo(StatutEcheance.A_VENIR));
    }

    @Test
    void echeances_qui_ne_soldent_pas_le_prix_refusees() {
        assertThatThrownBy(() -> etablir(Montant.de(8_320), plan(Montant.de(12_000))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void apport_insuffisant_refuse() {
        assertThatThrownBy(() -> etablir(Montant.de(8_000), plan(Montant.de(12_800))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rangs_non_consecutifs_refuses() {
        List<Echeance> echeances = List.of(
                Echeance.reconstituer(UUID.randomUUID(), 1, LocalDate.of(2026, 11, 25), Montant.de(6_240),
                        StatutEcheance.A_VENIR),
                Echeance.reconstituer(UUID.randomUUID(), 3, LocalDate.of(2026, 12, 25), Montant.de(6_240),
                        StatutEcheance.A_VENIR));

        assertThatThrownBy(() -> Contrat.reconstituer(UUID.randomUUID(), "SEH-2026-000001", UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Galaxy A35", PRIX, Montant.de(8_320),
                echeances, LE)).isInstanceOf(IllegalArgumentException.class);
    }
}
