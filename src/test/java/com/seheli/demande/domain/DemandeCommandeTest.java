package com.seheli.demande.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DemandeCommandeTest {

    private static final Instant PLUS_TARD = DemandesFixtures.LE.plusSeconds(3_600);
    private static final PrealablesCommande REUNIS = new PrealablesCommande(true, true);

    private static Demande dossier(StatutDossier statut) {
        return DemandesFixtures.decidee(UUID.randomUUID(), DemandesFixtures.termes(), statut, "analyste");
    }

    @Test
    void apport_encaisse_et_autorisation_acceptee_font_passer_a_apport_et_autorisation() {
        Demande d = dossier(StatutDossier.ACCEPTEE);

        ChangementStatut c = d.constaterApportEtAutorisation(REUNIS, PLUS_TARD);

        assertThat(c).isEqualTo(new ChangementStatut(StatutDossier.ACCEPTEE, StatutDossier.APPORT_ET_AUTORISATION));
        assertThat(d.statut()).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
        assertThat(d.statutChangeLe()).isEqualTo(PLUS_TARD);
    }

    @Test
    void jamais_apport_et_autorisation_sans_l_apport_ou_sans_l_autorisation() {
        for (PrealablesCommande incomplet : new PrealablesCommande[] {
                new PrealablesCommande(true, false), new PrealablesCommande(false, true), PrealablesCommande.AUCUN}) {
            Demande d = dossier(StatutDossier.ACCEPTEE);
            assertThatThrownBy(() -> d.constaterApportEtAutorisation(incomplet, PLUS_TARD))
                    .isInstanceOf(PrealablesManquantsException.class);
            assertThat(d.statut()).isEqualTo(StatutDossier.ACCEPTEE);
        }
    }

    @Test
    void apport_et_autorisation_seulement_depuis_acceptee() {
        Demande d = dossier(StatutDossier.EN_ANALYSE);
        assertThatThrownBy(() -> d.constaterApportEtAutorisation(REUNIS, PLUS_TARD))
                .isInstanceOf(TransitionInterditeException.class);
    }

    @Test
    void une_autre_personne_que_l_analyste_valide_le_paiement_fournisseur() {
        Demande d = dossier(StatutDossier.APPORT_ET_AUTORISATION);

        ChangementStatut c = d.commander("admin", REUNIS, PLUS_TARD);

        assertThat(c).isEqualTo(new ChangementStatut(StatutDossier.APPORT_ET_AUTORISATION, StatutDossier.COMMANDEE));
        assertThat(d.statut()).isEqualTo(StatutDossier.COMMANDEE);
    }

    @Test
    void l_analyste_qui_a_decide_ne_valide_pas_le_paiement_fournisseur() {
        Demande d = dossier(StatutDossier.APPORT_ET_AUTORISATION);

        assertThatThrownBy(() -> d.commander("analyste", REUNIS, PLUS_TARD))
                .isInstanceOf(SeparationDesTachesException.class);
        assertThat(d.statut()).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
    }

    @Test
    void jamais_commandee_sans_apport_ni_autorisation() {
        Demande d = dossier(StatutDossier.APPORT_ET_AUTORISATION);

        assertThatThrownBy(() -> d.commander("admin", new PrealablesCommande(true, false), PLUS_TARD))
                .isInstanceOf(PrealablesManquantsException.class);
        assertThat(d.statut()).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
    }

    @Test
    void un_dossier_seulement_accepte_ne_peut_pas_etre_commande() {
        Demande d = dossier(StatutDossier.ACCEPTEE);

        assertThatThrownBy(() -> d.commander("admin", REUNIS, PLUS_TARD))
                .isInstanceOf(TransitionInterditeException.class);
    }
}
