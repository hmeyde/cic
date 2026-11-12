package com.cic.demande.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SuiviDossierTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");

    private static SuiviDossier suivi(StatutDossier statut) {
        Demande demande = Demande.reconstituer(UUID.randomUUID(), UUID.randomUUID(), DemandesFixtures.termes(),
                DemandesFixtures.score(), DemandesFixtures.LE, statut, Instant.parse("2026-11-14T23:30:00Z"));
        return SuiviDossier.de(demande, NOUAKCHOTT, 7);
    }

    @Test
    void en_analyse_cic_etudie_la_demande() {
        SuiviDossier s = suivi(StatutDossier.EN_ANALYSE);

        assertThat(s.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
        assertThat(s.prochaine()).isEqualTo(ProchaineEtape.ATTENDRE_DECISION);
        assertThat(s.prochaine().parClient()).isFalse();
        assertThat(s.dateLimite()).isEmpty();
        assertThat(s.etapes()).extracting(EtapeSuivie::etat).containsExactly(EtatEtape.EN_COURS, EtatEtape.A_VENIR,
                EtatEtape.A_VENIR, EtatEtape.A_VENIR, EtatEtape.A_VENIR);
    }

    @Test
    void acceptee_le_client_paie_l_apport_avant_la_date_limite() {
        SuiviDossier s = suivi(StatutDossier.ACCEPTEE);

        assertThat(s.prochaine()).isEqualTo(ProchaineEtape.PAYER_APPORT_ET_SIGNER_AUTORISATION);
        assertThat(s.prochaine().parClient()).isTrue();
        assertThat(s.statutDepuis()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(s.dateLimite()).contains(LocalDate.of(2026, 11, 21));
        assertThat(s.etapes()).extracting(EtapeSuivie::etat).containsExactly(EtatEtape.FAITE, EtatEtape.EN_COURS,
                EtatEtape.A_VENIR, EtatEtape.A_VENIR, EtatEtape.A_VENIR);
    }

    @Test
    void refusee_s_arrete_a_l_etude() {
        SuiviDossier s = suivi(StatutDossier.REFUSEE);

        assertThat(s.prochaine()).isEqualTo(ProchaineEtape.NOUVELLE_DEMANDE_POSSIBLE);
        assertThat(s.etapes()).extracting(EtapeSuivie::etat).containsExactly(EtatEtape.ARRETEE, EtatEtape.A_VENIR,
                EtatEtape.A_VENIR, EtatEtape.A_VENIR, EtatEtape.A_VENIR);
    }

    @Test
    void annulee_s_arrete_a_l_apport() {
        assertThat(suivi(StatutDossier.ANNULEE).etapes()).extracting(EtapeSuivie::etat).containsExactly(
                EtatEtape.FAITE, EtatEtape.ARRETEE, EtatEtape.A_VENIR, EtatEtape.A_VENIR, EtatEtape.A_VENIR);
    }

    @Test
    void en_retard_le_client_regularise() {
        SuiviDossier s = suivi(StatutDossier.EN_RETARD);

        assertThat(s.prochaine()).isEqualTo(ProchaineEtape.REGULARISER_ECHEANCE);
        assertThat(s.etapes()).extracting(EtapeSuivie::etat).containsExactly(EtatEtape.FAITE, EtatEtape.FAITE,
                EtatEtape.FAITE, EtatEtape.FAITE, EtatEtape.EN_COURS);
    }

    @Test
    void soldee_toutes_les_etapes_sont_faites() {
        assertThat(suivi(StatutDossier.SOLDEE).etapes()).extracting(EtapeSuivie::etat).containsOnly(EtatEtape.FAITE);
    }

    @ParameterizedTest
    @EnumSource(StatutDossier.class)
    void chaque_statut_a_une_prochaine_etape_et_cinq_etapes(StatutDossier statut) {
        SuiviDossier s = suivi(statut);

        assertThat(s.prochaine()).isNotNull();
        assertThat(s.etapes()).extracting(EtapeSuivie::etape).containsExactly(EtapeDossier.values());
        assertThat(s.dateLimite().isPresent()).isEqualTo(statut == StatutDossier.ACCEPTEE);
    }

    @Test
    void delai_invalide() {
        Demande demande = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.ACCEPTEE);
        assertThatThrownBy(() -> SuiviDossier.de(demande, NOUAKCHOTT, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
