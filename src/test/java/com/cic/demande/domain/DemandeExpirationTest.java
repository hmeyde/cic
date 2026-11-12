package com.cic.demande.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DemandeExpirationTest {

    private static final Duration SEPT_JOURS = Duration.ofDays(7);
    private static final Instant ACCEPTEE_LE = DemandesFixtures.LE;

    private static Demande acceptee() {
        return DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.ACCEPTEE);
    }

    @Test
    void le_delai_est_depasse_sept_jours_apres_l_acceptation() {
        Demande d = acceptee();

        assertThat(d.delaiApportEtAutorisationDepasse(SEPT_JOURS, ACCEPTEE_LE.plus(SEPT_JOURS).minusSeconds(1)))
                .isFalse();
        assertThat(d.delaiApportEtAutorisationDepasse(SEPT_JOURS, ACCEPTEE_LE.plus(SEPT_JOURS))).isTrue();
    }

    @Test
    void un_dossier_qui_n_est_plus_acceptee_n_expire_pas() {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.APPORT_ET_AUTORISATION);

        assertThat(d.delaiApportEtAutorisationDepasse(SEPT_JOURS, ACCEPTEE_LE.plus(Duration.ofDays(30)))).isFalse();
    }

    @Test
    void un_dossier_expire_passe_en_annulee() {
        Demande d = acceptee();
        Instant le = ACCEPTEE_LE.plus(Duration.ofDays(8));

        ChangementStatut changement = d.annulerFauteApportOuAutorisation(SEPT_JOURS, le);

        assertThat(changement).isEqualTo(new ChangementStatut(StatutDossier.ACCEPTEE, StatutDossier.ANNULEE));
        assertThat(d.statut()).isEqualTo(StatutDossier.ANNULEE);
        assertThat(d.statutChangeLe()).isEqualTo(le);
        assertThat(d.estActive()).isFalse();
    }

    @Test
    void annuler_avant_la_fin_du_delai_est_refuse() {
        Demande d = acceptee();

        assertThatThrownBy(() -> d.annulerFauteApportOuAutorisation(SEPT_JOURS, ACCEPTEE_LE.plus(Duration.ofDays(6))))
                .isInstanceOf(DelaiNonDepasseException.class);
        assertThat(d.statut()).isEqualTo(StatutDossier.ACCEPTEE);
    }

    @Test
    void annuler_un_dossier_qui_n_est_pas_acceptee_est_une_transition_interdite() {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.EN_ANALYSE);

        assertThatThrownBy(() -> d.annulerFauteApportOuAutorisation(SEPT_JOURS, ACCEPTEE_LE.plus(Duration.ofDays(8))))
                .isInstanceOf(TransitionInterditeException.class);
    }
}
