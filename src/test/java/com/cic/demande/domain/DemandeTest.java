package com.cic.demande.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;

class DemandeTest {

    private static final UUID CLIENT = UUID.randomUUID();

    @Test
    void une_demande_soumise_attend_d_etre_mise_en_analyse() {
        Demande demande = Demande.soumettre(CLIENT, DemandesFixtures.termes(), DemandesFixtures.score(),
                DemandesFixtures.LE);

        assertThat(demande.statut()).isEqualTo(StatutDossier.DEMANDE_SOUMISE);
        assertThat(demande.soumiseLe()).isEqualTo(DemandesFixtures.LE);
        assertThat(demande.estActive()).isTrue();
        assertThat(demande.score().versionGrille()).isEqualTo("v1");
    }

    @Test
    void mise_en_analyse_par_la_machine_a_etats() {
        Demande demande = Demande.soumettre(CLIENT, DemandesFixtures.termes(), DemandesFixtures.score(),
                DemandesFixtures.LE);
        Instant plusTard = DemandesFixtures.LE.plusSeconds(1);

        ChangementStatut changement = demande.mettreEnAnalyse(plusTard);

        assertThat(changement).isEqualTo(new ChangementStatut(StatutDossier.DEMANDE_SOUMISE, StatutDossier.EN_ANALYSE));
        assertThat(demande.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
        assertThat(demande.statutChangeLe()).isEqualTo(plusTard);
    }

    @Test
    void une_demande_deja_en_analyse_n_y_repasse_pas() {
        Demande demande = DemandesFixtures.demande(CLIENT, StatutDossier.EN_ANALYSE);

        assertThatThrownBy(() -> demande.mettreEnAnalyse(DemandesFixtures.LE))
                .isInstanceOf(TransitionInterditeException.class);
        assertThat(demande.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
    }

    @Test
    void termes_de_l_exemple_chiffre() {
        TermesDemande termes = DemandesFixtures.termes();

        assertThat(termes.resteAFinancer()).isEqualTo(Montant.de(12_480));
        assertThat(termes.mensualiteMaximale()).isEqualTo(Montant.de(2_080));
    }

    @Test
    void les_termes_doivent_equilibrer_le_prix_de_vente() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new TermesDemande(id, id, "Galaxy", Montant.de(20_000), Montant.de(18_400),
                Montant.de(20_800), Montant.de(8_320), Montant.de(8_320), 6, Montant.de(2_000), Montant.de(2_000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void apport_hors_bornes_refuse_dans_les_termes() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new TermesDemande(id, id, "Galaxy", Montant.de(20_000), Montant.de(18_400),
                Montant.de(20_800), Montant.de(8_320), Montant.de(8_000), 6, Montant.de(2_133), Montant.de(2_135)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dossiers_actifs_et_en_retard() {
        assertThat(DemandesFixtures.inactifs()).noneMatch(StatutDossier::estActif);
        assertThat(StatutDossier.RECOUVREMENT.estActif()).isTrue();
        assertThat(StatutDossier.EN_ANALYSE.estActif()).isTrue();
        assertThat(StatutDossier.EN_RETARD.estEnRetard()).isTrue();
        assertThat(StatutDossier.RECOUVREMENT.estEnRetard()).isTrue();
        assertThat(StatutDossier.EN_REMBOURSEMENT.estEnRetard()).isFalse();
    }
}
