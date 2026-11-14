package com.seheli.recouvrement.application;

import static com.seheli.recouvrement.application.RappelsDeTest.DEMAIN;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.identite.domain.Client;
import com.seheli.notification.domain.Canal;
import com.seheli.notification.domain.Message;
import com.seheli.recouvrement.domain.Relance;
import com.seheli.recouvrement.domain.TypeRelance;

class RappelPrelevementServiceTest {

    private final RappelsDeTest r = new RappelsDeTest();

    @Test
    void la_veille_du_prelevement_le_client_recoit_un_sms_avec_un_lien_wa_me_prerempli() {
        Client client = r.client("36123456");
        Contrat contrat = r.contrat(client, StatutDossier.EN_REMBOURSEMENT, DEMAIN);

        assertThat(r.service.echeancesARappeler()).extracting(e -> e.echeance().rang()).containsExactly(1);
        assertThat(r.service.rappeler(r.service.echeancesARappeler().getFirst())).isTrue();

        assertThat(r.envoyes).hasSize(1);
        Message m = r.envoyes.getFirst();
        assertThat(m.telephone()).isEqualTo("36123456");
        assertThat(m.canal()).isEqualTo(Canal.SMS);
        assertThat(m.texte())
                .startsWith("Seheli : rappel, votre mensualité 1/6 de 2080 MRU (contrat " + contrat.numero()
                        + ") sera prélevée le 13/11/2026.")
                .contains("https://wa.me/22220000000?text=Bonjour%20Seheli%2C%20j%27ai%20une%20question%20sur%20ma%20"
                        + "mensualit%C3%A9%201%2F6%20du%2013%2F11%2F2026%20%28contrat%20" + contrat.numero()
                        + "%29.");
        Relance relance = r.relances.toutes().getFirst();
        assertThat(relance.type()).isEqualTo(TypeRelance.AVANT_PRELEVEMENT);
        assertThat(relance.echeanceId()).isEqualTo(contrat.echeance(1).orElseThrow().id());
        assertThat(relance.envoyeeLe()).isEqualTo(RappelsDeTest.AUJOURDHUI);
    }

    @Test
    void une_echeance_n_est_rappelee_qu_une_fois() {
        r.contrat(r.client("36123456"), StatutDossier.EN_REMBOURSEMENT, DEMAIN);
        EcheanceAPrelever e = r.service.echeancesARappeler().getFirst();

        assertThat(r.service.rappeler(e)).isTrue();
        assertThat(r.service.rappeler(e)).isFalse();
        assertThat(r.envoyes).hasSize(1);
    }

    @Test
    void seules_les_echeances_de_demain_sont_rappelees_ni_aujourd_hui_ni_apres_demain() {
        r.contrat(r.client("36000001"), StatutDossier.EN_REMBOURSEMENT, DEMAIN.minusDays(1));
        r.contrat(r.client("36000002"), StatutDossier.EN_REMBOURSEMENT, DEMAIN.plusDays(1));

        assertThat(r.service.echeancesARappeler()).isEmpty();
    }

    @Test
    void pas_de_rappel_tant_que_les_prelevements_ne_sont_pas_en_place_ni_apres_le_solde() {
        r.contrat(r.client("36000001"), StatutDossier.ACCEPTEE, DEMAIN);
        r.contrat(r.client("36000002"), StatutDossier.ANNULEE, DEMAIN);
        r.contrat(r.client("36000003"), StatutDossier.SOLDEE, DEMAIN);

        assertThat(r.service.echeancesARappeler()).hasSize(3).allSatisfy(e ->
                assertThat(r.service.rappeler(e)).isFalse());
        assertThat(r.envoyes).isEmpty();
        assertThat(r.relances.toutes()).isEmpty();
    }

    @Test
    void un_dossier_en_retard_recoit_aussi_le_rappel_de_sa_prochaine_echeance() {
        r.contrat(r.client("36000001"), StatutDossier.EN_RETARD, DEMAIN);

        assertThat(r.service.rappeler(r.service.echeancesARappeler().getFirst())).isTrue();
    }
}
