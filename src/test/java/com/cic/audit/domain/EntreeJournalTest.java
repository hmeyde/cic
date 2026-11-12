package com.cic.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EntreeJournalTest {

    private static final Instant LE = Instant.parse("2026-11-12T10:00:00Z");
    private static final ObjetAudite DOSSIER = new ObjetAudite("DEMANDE", UUID.randomUUID());

    @Test
    void transition_avec_statut_avant_et_apres() {
        EntreeJournal e = EntreeJournal.transition(LE, Acteur.backOffice("analyste"), "REFUS", DOSSIER, "EN_ANALYSE",
                "REFUSEE", "  Revenus insuffisants  ");

        assertThat(e.avant()).contains("EN_ANALYSE");
        assertThat(e.apres()).contains("REFUSEE");
        assertThat(e.detail()).contains("Revenus insuffisants");
        assertThat(e.acteur().type()).isEqualTo(TypeActeur.BACK_OFFICE);
    }

    @Test
    void creation_sans_statut_avant_et_action_sans_transition() {
        EntreeJournal creation = EntreeJournal.transition(LE, Acteur.client(UUID.randomUUID()), "SOUMISSION", DOSSIER,
                null, "DEMANDE_SOUMISE", null);
        EntreeJournal action = EntreeJournal.action(LE, Acteur.systeme("relances"), "RELANCE", DOSSIER, " ");

        assertThat(creation.avant()).isEmpty();
        assertThat(creation.apres()).contains("DEMANDE_SOUMISE");
        assertThat(action.avant()).isEmpty();
        assertThat(action.apres()).isEmpty();
        assertThat(action.detail()).isEmpty();
    }

    @Test
    void le_texte_de_journalisation_ne_contient_pas_le_detail() {
        EntreeJournal e = EntreeJournal.action(LE, Acteur.backOffice("analyste"), "DEMANDE_JUSTIFICATIF", DOSSIER,
                "Bulletin de paie de M. Ould Ahmed");

        assertThat(e.toString()).doesNotContain("Ould Ahmed").contains("DEMANDE_JUSTIFICATIF");
    }

    @Test
    void champs_vides_ou_trop_longs_refuses() {
        assertThatThrownBy(() -> EntreeJournal.action(LE, Acteur.backOffice("a"), " ", DOSSIER, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntreeJournal.action(LE, Acteur.backOffice("a"), "X", DOSSIER,
                "x".repeat(EntreeJournal.LONGUEUR_DETAIL_MAX + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Acteur.backOffice(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ObjetAudite("", UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);
    }
}
