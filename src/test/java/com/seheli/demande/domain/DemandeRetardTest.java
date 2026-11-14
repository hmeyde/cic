package com.seheli.demande.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DemandeRetardTest {

    private static final Instant LE = Instant.parse("2026-11-26T08:00:00Z");

    @Test
    void un_dossier_en_remboursement_passe_en_retard() {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.EN_REMBOURSEMENT);

        ChangementStatut c = d.constaterRetard(LE);

        assertThat(c).isEqualTo(new ChangementStatut(StatutDossier.EN_REMBOURSEMENT, StatutDossier.EN_RETARD));
        assertThat(d.statut()).isEqualTo(StatutDossier.EN_RETARD);
        assertThat(d.statutChangeLe()).isEqualTo(LE);
        assertThat(d.estActive()).isTrue();
    }

    @Test
    void seul_un_dossier_en_remboursement_passe_en_retard() {
        for (StatutDossier statut : EnumSet.complementOf(EnumSet.of(StatutDossier.EN_REMBOURSEMENT))) {
            Demande d = DemandesFixtures.demande(UUID.randomUUID(), statut);
            assertThatThrownBy(() -> d.constaterRetard(LE)).as(statut.name())
                    .isInstanceOf(TransitionInterditeException.class);
        }
    }
}
