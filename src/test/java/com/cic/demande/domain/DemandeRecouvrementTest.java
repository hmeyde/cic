package com.cic.demande.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DemandeRecouvrementTest {

    private static final Instant LE = Instant.parse("2026-11-26T08:00:00Z");
    private static final SituationEcheances A_JOUR = new SituationEcheances(0, 4);
    private static final SituationEcheances EN_RETARD = new SituationEcheances(1, 5);
    private static final SituationEcheances TOUT_PAYE = new SituationEcheances(0, 0);

    private static Demande demande(StatutDossier statut) {
        return DemandesFixtures.demande(UUID.randomUUID(), statut);
    }

    @Test
    void un_dossier_en_retard_dont_les_echeances_en_retard_sont_payees_est_regularise() {
        Demande d = demande(StatutDossier.EN_RETARD);

        ChangementStatut c = d.regulariser(A_JOUR, LE);

        assertThat(c).isEqualTo(new ChangementStatut(StatutDossier.EN_RETARD, StatutDossier.EN_REMBOURSEMENT));
        assertThat(d.statut()).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
        assertThat(d.statutChangeLe()).isEqualTo(LE);
    }

    @Test
    void pas_de_regularisation_tant_qu_une_echeance_est_en_retard() {
        Demande d = demande(StatutDossier.EN_RETARD);

        assertThatThrownBy(() -> d.regulariser(EN_RETARD, LE)).isInstanceOf(RegularisationImpossibleException.class);
        assertThat(d.statut()).isEqualTo(StatutDossier.EN_RETARD);
    }

    @Test
    void un_dossier_en_retard_passe_en_recouvrement_avec_un_motif() {
        Demande d = demande(StatutDossier.EN_RETARD);

        ChangementStatut c = d.passerEnRecouvrement("  Injoignable depuis 3 semaines ", LE);

        assertThat(c.vers()).isEqualTo(StatutDossier.RECOUVREMENT);
        assertThat(d.statut()).isEqualTo(StatutDossier.RECOUVREMENT);
        assertThat(d.estActive()).isTrue();
    }

    @Test
    void le_passage_en_recouvrement_exige_un_motif_d_au_plus_500_caracteres() {
        Demande d = demande(StatutDossier.EN_RETARD);

        assertThatThrownBy(() -> d.passerEnRecouvrement(" ", LE))
                .isInstanceOf(MotifRecouvrementInvalideException.class);
        assertThatThrownBy(() -> d.passerEnRecouvrement("x".repeat(501), LE))
                .isInstanceOf(MotifRecouvrementInvalideException.class);
        assertThat(d.statut()).isEqualTo(StatutDossier.EN_RETARD);
    }

    @Test
    void un_dossier_en_remboursement_entierement_paye_est_solde() {
        Demande d = demande(StatutDossier.EN_REMBOURSEMENT);

        assertThat(d.solder(TOUT_PAYE, LE).vers()).isEqualTo(StatutDossier.SOLDEE);
        assertThat(d.estActive()).isFalse();
    }

    @Test
    void pas_de_solde_tant_qu_une_echeance_reste_a_payer() {
        Demande d = demande(StatutDossier.EN_REMBOURSEMENT);

        assertThatThrownBy(() -> d.solder(A_JOUR, LE)).isInstanceOf(SoldeImpossibleException.class);
        assertThat(d.statut()).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
    }

    @Test
    void ces_transitions_sont_interdites_depuis_tout_autre_statut() {
        for (StatutDossier statut : EnumSet.complementOf(EnumSet.of(StatutDossier.EN_RETARD))) {
            assertThatThrownBy(() -> demande(statut).regulariser(A_JOUR, LE)).as(statut.name())
                    .isInstanceOf(TransitionInterditeException.class);
            assertThatThrownBy(() -> demande(statut).passerEnRecouvrement("Motif", LE)).as(statut.name())
                    .isInstanceOf(TransitionInterditeException.class);
        }
        for (StatutDossier statut : EnumSet.complementOf(EnumSet.of(StatutDossier.EN_REMBOURSEMENT))) {
            assertThatThrownBy(() -> demande(statut).solder(TOUT_PAYE, LE)).as(statut.name())
                    .isInstanceOf(TransitionInterditeException.class);
        }
    }

    @Test
    void une_situation_incoherente_est_refusee() {
        assertThatThrownBy(() -> new SituationEcheances(2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SituationEcheances(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
