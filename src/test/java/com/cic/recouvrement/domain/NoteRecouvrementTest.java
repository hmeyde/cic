package com.cic.recouvrement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NoteRecouvrementTest {

    private static final Instant LE = Instant.parse("2026-11-12T10:00:00Z");

    @Test
    void une_note_est_datee_signee_et_debarrassee_de_ses_espaces() {
        UUID dossier = UUID.randomUUID();

        NoteRecouvrement n = NoteRecouvrement.ecrire(dossier, "recouvrement", "  Client joint, paiera vendredi.\n", LE);

        assertThat(n.demandeId()).isEqualTo(dossier);
        assertThat(n.auteur()).isEqualTo("recouvrement");
        assertThat(n.texte()).isEqualTo("Client joint, paiera vendredi.");
        assertThat(n.ecriteLe()).isEqualTo(LE);
        assertThat(n.toString()).doesNotContain("paiera");
    }

    @Test
    void une_note_vide_ou_trop_longue_est_refusee() {
        UUID dossier = UUID.randomUUID();

        assertThatThrownBy(() -> NoteRecouvrement.ecrire(dossier, "r", null, LE))
                .isInstanceOf(NoteInvalideException.class);
        assertThatThrownBy(() -> NoteRecouvrement.ecrire(dossier, "r", "   ", LE))
                .isInstanceOf(NoteInvalideException.class);
        assertThatThrownBy(() -> NoteRecouvrement.ecrire(dossier, "r", "x".repeat(1001), LE))
                .isInstanceOf(NoteInvalideException.class);
        assertThat(NoteRecouvrement.ecrire(dossier, "r", "x".repeat(1000), LE).texte()).hasSize(1000);
    }
}
