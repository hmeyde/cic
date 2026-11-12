package com.cic.recouvrement.adapter.in.scheduling;

import static com.cic.recouvrement.application.RappelsDeTest.DEMAIN;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cic.demande.domain.StatutDossier;
import com.cic.recouvrement.application.RappelsDeTest;

class RappelPrelevementJobTest {

    private final RappelsDeTest r = new RappelsDeTest();
    private final RappelPrelevementJob job = new RappelPrelevementJob(r.service);

    @Test
    void le_job_envoie_les_rappels_de_demain_une_seule_fois() {
        r.contrat(r.client("36000001"), StatutDossier.EN_REMBOURSEMENT, DEMAIN);
        r.contrat(r.client("36000002"), StatutDossier.COMMANDEE, DEMAIN);
        r.contrat(r.client("36000003"), StatutDossier.EN_REMBOURSEMENT, DEMAIN.plusDays(5));

        assertThat(job.envoyerRappels()).isEqualTo(2);
        assertThat(job.envoyerRappels()).isZero();
        assertThat(r.envoyes).extracting(m -> m.telephone()).containsExactly("36000001", "36000002");
    }
}
