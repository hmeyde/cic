package com.seheli.demande.domain;

import static com.seheli.demande.domain.EtatEtape.A_VENIR;
import static com.seheli.demande.domain.EtatEtape.ARRETEE;
import static com.seheli.demande.domain.EtatEtape.EN_ALERTE;
import static com.seheli.demande.domain.EtatEtape.EN_COURS;
import static com.seheli.demande.domain.EtatEtape.FAITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class SuiviDossierTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");

    private static SuiviDossier suivi(StatutDossier statut) {
        Demande demande = Demande.reconstituer(UUID.randomUUID(), UUID.randomUUID(), DemandesFixtures.termes(),
                DemandesFixtures.score(), DemandesFixtures.LE, statut, Instant.parse("2026-11-14T23:30:00Z"));
        return SuiviDossier.de(demande, NOUAKCHOTT, 7);
    }

    @Test
    void la_frise_compte_huit_jalons_dans_l_ordre_du_suivi() {
        assertThat(EtapeDossier.values()).containsExactly(EtapeDossier.SOUMISE, EtapeDossier.EN_ANALYSE,
                EtapeDossier.ACCEPTEE, EtapeDossier.APPORT_ET_AUTORISATION, EtapeDossier.COMMANDEE,
                EtapeDossier.LIVREE, EtapeDossier.EN_REMBOURSEMENT, EtapeDossier.SOLDEE);
    }

    /** Chaque statut du cycle de vie met en avant son jalon, avec le bon état pour chacun des huit. */
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "DEMANDE_SOUMISE,        SOUMISE,                EN_COURS  A_VENIR   A_VENIR   A_VENIR   A_VENIR   A_VENIR   A_VENIR   A_VENIR",
            "EN_ANALYSE,             EN_ANALYSE,             FAITE     EN_COURS  A_VENIR   A_VENIR   A_VENIR   A_VENIR   A_VENIR   A_VENIR",
            "REFUSEE,                ACCEPTEE,               FAITE     FAITE     ARRETEE   A_VENIR   A_VENIR   A_VENIR   A_VENIR   A_VENIR",
            "ACCEPTEE,               ACCEPTEE,               FAITE     FAITE     EN_COURS  A_VENIR   A_VENIR   A_VENIR   A_VENIR   A_VENIR",
            "ANNULEE,                APPORT_ET_AUTORISATION, FAITE     FAITE     FAITE     ARRETEE   A_VENIR   A_VENIR   A_VENIR   A_VENIR",
            "APPORT_ET_AUTORISATION, APPORT_ET_AUTORISATION, FAITE     FAITE     FAITE     EN_COURS  A_VENIR   A_VENIR   A_VENIR   A_VENIR",
            "COMMANDEE,              COMMANDEE,              FAITE     FAITE     FAITE     FAITE     EN_COURS  A_VENIR   A_VENIR   A_VENIR",
            "LIVREE,                 LIVREE,                 FAITE     FAITE     FAITE     FAITE     FAITE     EN_COURS  A_VENIR   A_VENIR",
            "EN_REMBOURSEMENT,       EN_REMBOURSEMENT,       FAITE     FAITE     FAITE     FAITE     FAITE     FAITE     EN_COURS  A_VENIR",
            "EN_RETARD,              EN_REMBOURSEMENT,       FAITE     FAITE     FAITE     FAITE     FAITE     FAITE     EN_ALERTE A_VENIR",
            "RECOUVREMENT,           EN_REMBOURSEMENT,       FAITE     FAITE     FAITE     FAITE     FAITE     FAITE     EN_ALERTE A_VENIR",
            "SOLDEE,                 SOLDEE,                 FAITE     FAITE     FAITE     FAITE     FAITE     FAITE     FAITE     FAITE",
    })
    void chaque_statut_met_en_avant_son_jalon(StatutDossier statut, EtapeDossier courante, String etats) {
        SuiviDossier s = suivi(statut);

        assertThat(s.courante()).isEqualTo(courante);
        assertThat(s.etapes()).extracting(EtapeSuivie::etape).containsExactly(EtapeDossier.values());
        assertThat(s.etapes()).extracting(e -> e.etat().name()).containsExactly(etats.trim().split("\\s+"));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "DEMANDE_SOUMISE, ATTENDRE_DECISION, false",
            "EN_ANALYSE, ATTENDRE_DECISION, false",
            "ACCEPTEE, PAYER_APPORT_ET_SIGNER_AUTORISATION, true",
            "REFUSEE, NOUVELLE_DEMANDE_POSSIBLE, true",
            "ANNULEE, NOUVELLE_DEMANDE_POSSIBLE, true",
            "APPORT_ET_AUTORISATION, ATTENDRE_COMMANDE, false",
            "COMMANDEE, RECEVOIR_ET_CONFIRMER_REMISE, true",
            "LIVREE, ATTENDRE_PREMIER_PRELEVEMENT, false",
            "EN_REMBOURSEMENT, PROVISIONNER_COMPTE, true",
            "EN_RETARD, REGULARISER_ECHEANCE, true",
            "RECOUVREMENT, CONTACTER_CIC, true",
            "SOLDEE, NOUVELLE_DEMANDE_POSSIBLE, true",
    })
    void chaque_statut_a_sa_prochaine_action(StatutDossier statut, ProchaineEtape prochaine, boolean parClient) {
        SuiviDossier s = suivi(statut);

        assertThat(s.prochaine()).isEqualTo(prochaine);
        assertThat(s.prochaine().parClient()).isEqualTo(parClient);
    }

    @Test
    void acceptee_le_client_paie_l_apport_avant_la_date_limite() {
        SuiviDossier s = suivi(StatutDossier.ACCEPTEE);

        assertThat(s.statutDepuis()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(s.dateLimite()).contains(LocalDate.of(2026, 11, 21));
        assertThat(s.etapes()).extracting(EtapeSuivie::etat).containsExactly(FAITE, FAITE, EN_COURS, A_VENIR,
                A_VENIR, A_VENIR, A_VENIR, A_VENIR);
    }

    @Test
    void compte_a_rebours_des_sept_jours() {
        SuiviDossier s = suivi(StatutDossier.ACCEPTEE);

        assertThat(s.joursRestants(LocalDate.of(2026, 11, 14))).contains(7L);
        assertThat(s.pourcentageDelaiEcoule(LocalDate.of(2026, 11, 14))).contains(0);
        assertThat(s.joursRestants(LocalDate.of(2026, 11, 16))).contains(5L);
        assertThat(s.pourcentageDelaiEcoule(LocalDate.of(2026, 11, 16))).contains(29);
        assertThat(s.joursRestants(LocalDate.of(2026, 11, 21))).contains(0L);
        assertThat(s.joursRestants(LocalDate.of(2026, 11, 23))).contains(0L);
        assertThat(s.pourcentageDelaiEcoule(LocalDate.of(2026, 11, 23))).contains(100);
    }

    @ParameterizedTest
    @EnumSource(value = StatutDossier.class, names = "ACCEPTEE", mode = EnumSource.Mode.EXCLUDE)
    void pas_de_compte_a_rebours_hors_acceptee(StatutDossier statut) {
        SuiviDossier s = suivi(statut);

        assertThat(s.dateLimite()).isEmpty();
        assertThat(s.joursRestants(LocalDate.of(2026, 11, 16))).isEmpty();
        assertThat(s.pourcentageDelaiEcoule(LocalDate.of(2026, 11, 16))).isEmpty();
    }

    @Test
    void annulee_s_arrete_avant_l_apport() {
        assertThat(suivi(StatutDossier.ANNULEE).etapes()).extracting(EtapeSuivie::etat).containsExactly(FAITE, FAITE,
                FAITE, ARRETEE, A_VENIR, A_VENIR, A_VENIR, A_VENIR);
    }

    @Test
    void en_retard_le_remboursement_est_en_alerte() {
        SuiviDossier s = suivi(StatutDossier.EN_RETARD);

        assertThat(s.prochaine()).isEqualTo(ProchaineEtape.REGULARISER_ECHEANCE);
        assertThat(s.etapes().get(6)).isEqualTo(new EtapeSuivie(EtapeDossier.EN_REMBOURSEMENT, EN_ALERTE));
    }

    @Test
    void soldee_toutes_les_etapes_sont_faites() {
        SuiviDossier s = suivi(StatutDossier.SOLDEE);

        assertThat(s.etapes()).extracting(EtapeSuivie::etat).containsOnly(FAITE);
        assertThat(s.courante()).isEqualTo(EtapeDossier.SOLDEE);
    }

    @Test
    void delai_invalide() {
        Demande demande = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.ACCEPTEE);
        assertThatThrownBy(() -> SuiviDossier.de(demande, NOUAKCHOTT, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
