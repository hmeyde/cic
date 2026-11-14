package com.seheli.demande.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DemandeRemiseTest {

    private static final Instant PLUS_TARD = DemandesFixtures.LE.plusSeconds(3_600);

    private static Demande dossier(StatutDossier statut) {
        return DemandesFixtures.decidee(UUID.randomUUID(), DemandesFixtures.termes(), statut, "analyste");
    }

    @Test
    void la_remise_confirmee_fait_passer_a_livree_puis_en_remboursement() {
        Demande d = dossier(StatutDossier.COMMANDEE);

        assertThat(d.confirmerRemise(PLUS_TARD))
                .isEqualTo(new ChangementStatut(StatutDossier.COMMANDEE, StatutDossier.LIVREE));
        assertThat(d.statut()).isEqualTo(StatutDossier.LIVREE);

        assertThat(d.demarrerRemboursement(PLUS_TARD))
                .isEqualTo(new ChangementStatut(StatutDossier.LIVREE, StatutDossier.EN_REMBOURSEMENT));
        assertThat(d.statut()).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
        assertThat(d.statutChangeLe()).isEqualTo(PLUS_TARD);
        assertThat(d.estActive()).isTrue();
    }

    @Test
    void la_remise_n_est_confirmee_que_pour_un_produit_commande() {
        for (StatutDossier statut : EnumSet.complementOf(EnumSet.of(StatutDossier.COMMANDEE))) {
            Demande d = dossier(statut);
            assertThatThrownBy(() -> d.confirmerRemise(PLUS_TARD)).as(statut.name())
                    .isInstanceOf(TransitionInterditeException.class);
            assertThat(d.statut()).isEqualTo(statut);
        }
    }

    @Test
    void le_remboursement_ne_demarre_qu_apres_la_remise() {
        for (StatutDossier statut : EnumSet.complementOf(EnumSet.of(StatutDossier.LIVREE, StatutDossier.EN_RETARD))) {
            Demande d = dossier(statut);
            assertThatThrownBy(() -> d.demarrerRemboursement(PLUS_TARD)).as(statut.name())
                    .isInstanceOf(TransitionInterditeException.class);
            assertThat(d.statut()).isEqualTo(statut);
        }
    }
}
