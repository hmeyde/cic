package com.seheli.recouvrement.adapter.in.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.seheli.contrat.domain.Contrat;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.recouvrement.application.RappelsDeTest;

class RetardsEtRelancesJobTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 12);
    private final RappelsDeTest r = new RappelsDeTest();
    private final RetardsEtRelancesJob job = new RetardsEtRelancesJob(r.retards);

    @Test
    void le_job_detecte_les_retards_puis_relance_a_j_plus_3_sans_doublon() {
        Contrat hier = r.contrat(r.client("36000001"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.minusDays(1));
        Contrat j3 = r.contrat(r.client("36000002"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.minusDays(3));
        r.contrat(r.client("36000003"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI);

        assertThat(job.detecterRetards()).isEqualTo(2);
        assertThat(job.envoyerRelances()).isEqualTo(1);
        assertThat(job.detecterRetards()).isZero();
        assertThat(job.envoyerRelances()).isZero();

        assertThat(r.demandes.parId(hier.demandeId()).orElseThrow().statut()).isEqualTo(StatutDossier.EN_RETARD);
        assertThat(r.demandes.parId(j3.demandeId()).orElseThrow().statut()).isEqualTo(StatutDossier.EN_RETARD);
        assertThat(r.envoyes).extracting(m -> m.telephone()).containsExactly("36000002");
        assertThat(r.contrats.clientAUneEcheanceEnRetard(hier.clientId())).isTrue();
    }
}
