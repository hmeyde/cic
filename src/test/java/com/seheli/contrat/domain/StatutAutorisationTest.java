package com.seheli.contrat.domain;

import static com.seheli.contrat.domain.StatutAutorisation.ACCEPTEE;
import static com.seheli.contrat.domain.StatutAutorisation.ACTIVE;
import static com.seheli.contrat.domain.StatutAutorisation.A_SIGNER;
import static com.seheli.contrat.domain.StatutAutorisation.REVOQUEE;
import static com.seheli.contrat.domain.StatutAutorisation.SIGNEE;
import static com.seheli.contrat.domain.StatutAutorisation.TRANSMISE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatutAutorisationTest {

    @Test
    void le_cycle_de_vie_suit_signature_transmission_acceptation_activation() {
        assertThat(A_SIGNER.suivants()).containsExactly(SIGNEE);
        assertThat(SIGNEE.suivants()).containsExactlyInAnyOrder(TRANSMISE, REVOQUEE);
        assertThat(TRANSMISE.suivants()).containsExactlyInAnyOrder(ACCEPTEE, REVOQUEE);
        assertThat(ACCEPTEE.suivants()).containsExactlyInAnyOrder(ACTIVE, REVOQUEE);
        assertThat(ACTIVE.suivants()).containsExactly(REVOQUEE);
        assertThat(REVOQUEE.suivants()).isEmpty();
    }

    @Test
    void le_document_signe_ne_change_plus_une_fois_l_ordre_parti_a_la_banque() {
        assertThat(A_SIGNER.accepteDocumentSigne()).isTrue();
        assertThat(SIGNEE.accepteDocumentSigne()).isTrue();
        for (StatutAutorisation s : new StatutAutorisation[] {TRANSMISE, ACCEPTEE, ACTIVE, REVOQUEE}) {
            assertThat(s.accepteDocumentSigne()).as(s.name()).isFalse();
        }
    }
}
