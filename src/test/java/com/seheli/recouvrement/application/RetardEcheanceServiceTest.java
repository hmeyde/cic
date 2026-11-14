package com.seheli.recouvrement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.seheli.audit.domain.EntreeJournal;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.contrat.domain.StatutEcheance;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.identite.domain.Client;
import com.seheli.notification.domain.Message;
import com.seheli.recouvrement.domain.RelanceDue;
import com.seheli.recouvrement.domain.TypeRelance;

class RetardEcheanceServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 12);
    private final RappelsDeTest r = new RappelsDeTest();

    @Test
    void une_echeance_impayee_a_sa_date_passe_en_retard_avec_son_dossier() {
        Client client = r.client("36000001");
        Contrat contrat = r.contrat(client, StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.minusDays(1));

        assertThat(r.retards.echeancesImpayees()).extracting(e -> e.echeance().rang()).containsExactly(1);
        assertThat(r.retards.constaterRetard(r.retards.echeancesImpayees().getFirst())).isTrue();

        Contrat apres = r.contrats.parId(contrat.id()).orElseThrow();
        assertThat(apres.echeance(1).orElseThrow().statut()).isEqualTo(StatutEcheance.EN_RETARD);
        assertThat(apres.echeance(1).orElseThrow().montant()).isEqualTo(contrat.echeance(1).orElseThrow().montant());
        assertThat(apres.echeance(2).orElseThrow().statut()).isEqualTo(StatutEcheance.A_VENIR);
        assertThat(r.demandes.parId(contrat.demandeId()).orElseThrow().statut()).isEqualTo(StatutDossier.EN_RETARD);
        assertThat(r.journal.entrees()).singleElement().extracting(EntreeJournal::apres).asString()
                .contains("EN_RETARD");
        assertThat(r.evenements).singleElement().isInstanceOfSatisfying(StatutDossierChange.class,
                e -> assertThat(e.vers()).isEqualTo(StatutDossier.EN_RETARD));
        assertThat(r.retards.echeancesImpayees()).isEmpty();
    }

    @Test
    void une_echeance_du_jour_n_est_pas_encore_en_retard() {
        r.contrat(r.client("36000001"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI);

        assertThat(r.retards.echeancesImpayees()).isEmpty();
    }

    @Test
    void les_echeances_d_un_dossier_annule_ou_non_finance_ne_sont_jamais_en_retard() {
        r.contrat(r.client("36000001"), StatutDossier.ANNULEE, AUJOURDHUI.minusDays(5));
        r.contrat(r.client("36000002"), StatutDossier.ACCEPTEE, AUJOURDHUI.minusDays(5));

        assertThat(r.retards.echeancesImpayees()).hasSize(2).allSatisfy(e ->
                assertThat(r.retards.constaterRetard(e)).isFalse());
        assertThat(r.contrats.tous()).allSatisfy(c -> assertThat(c.echeances())
                .allMatch(e -> e.statut() == StatutEcheance.A_VENIR));
    }

    @Test
    void avant_la_livraison_l_echeance_passe_en_retard_puis_le_dossier_une_fois_le_remboursement_demarre() {
        Contrat contrat = r.contrat(r.client("36000001"), StatutDossier.COMMANDEE, AUJOURDHUI.minusDays(1));

        assertThat(r.retards.constaterRetard(r.retards.echeancesImpayees().getFirst())).isTrue();
        assertThat(r.demandes.parId(contrat.demandeId()).orElseThrow().statut()).isEqualTo(StatutDossier.COMMANDEE);

        Demande demande = r.demandes.parId(contrat.demandeId()).orElseThrow();
        demande.confirmerRemise(RappelsDeTest.AUJOURDHUI);
        demande.demarrerRemboursement(RappelsDeTest.AUJOURDHUI);
        r.demandes.enregistrer(demande);

        EcheanceAPrelever enRetard = r.retards.echeancesEnRetard().getFirst();
        assertThat(r.retards.rattraperDossier(enRetard)).isTrue();
        assertThat(r.retards.rattraperDossier(enRetard)).isFalse();
        assertThat(r.demandes.parId(contrat.demandeId()).orElseThrow().statut()).isEqualTo(StatutDossier.EN_RETARD);
    }

    @Test
    void relance_a_j_plus_3_avec_la_reference_masrvi_puis_a_j_plus_7_une_seule_fois_chacune() {
        Client client = r.client("36123456");
        Contrat j3 = r.contrat(client, StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.minusDays(3));
        Contrat j7 = r.contrat(r.client("36000002"), StatutDossier.EN_RETARD, AUJOURDHUI.minusDays(7));
        r.contrat(r.client("36000003"), StatutDossier.EN_RETARD, AUJOURDHUI.minusDays(2));
        r.retards.echeancesImpayees().forEach(r.retards::constaterRetard);

        assertThat(r.retards.relancesDues()).extracting(d -> d.echeance().contratId(), RelanceDue::type)
                .containsExactly(
                        tuple(j7.id(), TypeRelance.ECHEC_SECONDE),
                        tuple(j3.id(), TypeRelance.ECHEC_PREMIERE));
        r.retards.relancesDues().forEach(d -> assertThat(r.retards.relancer(d)).isTrue());
        r.retards.relancesDues().forEach(d -> assertThat(r.retards.relancer(d)).isFalse());

        assertThat(r.envoyes).hasSize(2);
        Message premiere = r.envoyes.get(1);
        assertThat(premiere.telephone()).isEqualTo("36123456");
        assertThat(premiere.texte())
                .startsWith("Seheli : votre mensualité 1/6 de 2080 MRU (contrat " + j3.numero()
                        + ") prévue le 09/11/2026 n'a pas pu être prélevée. Aucune pénalité ne s'applique")
                .contains("code marchand 012345, référence EC")
                .contains("https://wa.me/22220000000?text=");
        assertThat(r.envoyes.getFirst().texte()).startsWith("Seheli : 2e relance");
        assertThat(r.relances.toutes()).extracting(rel -> rel.type())
                .containsExactlyInAnyOrder(TypeRelance.ECHEC_SECONDE, TypeRelance.ECHEC_PREMIERE);
    }

    @Test
    void une_echeance_payee_entre_temps_n_est_pas_relancee() {
        Contrat contrat = r.contrat(r.client("36000001"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.minusDays(3));
        r.retards.echeancesImpayees().forEach(r.retards::constaterRetard);
        r.contrats.enregistrerStatut(contrat.id(), r.contrats.parId(contrat.id()).orElseThrow().echeance(1)
                .orElseThrow().payee());

        assertThat(r.retards.relancesDues()).isEmpty();
    }
}
