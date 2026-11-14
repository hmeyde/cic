package com.seheli.identite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

import com.seheli.identite.domain.CodeConnexion.Verification;

class CodeConnexionTest {

    private static final Instant EMISSION = Instant.parse("2026-11-11T09:00:00Z");
    private static final BiPredicate<String, String> EGAL = String::equals;

    private static CodeConnexion code() {
        return CodeConnexion.emettre("22123456", "123456", EMISSION, Duration.ofMinutes(5), 5);
    }

    @Test
    void un_code_correct_dans_le_delai_est_valide_une_seule_fois() {
        CodeConnexion code = code();

        assertThat(code.verifier("123456", EMISSION.plusSeconds(60), EGAL)).isEqualTo(Verification.VALIDE);
        assertThat(code.estUtilise()).isTrue();
        assertThat(code.verifier("123456", EMISSION.plusSeconds(61), EGAL)).isEqualTo(Verification.DEJA_UTILISE);
    }

    @Test
    void le_code_expire_au_bout_de_5_minutes() {
        CodeConnexion code = code();

        assertThat(code.verifier("123456", EMISSION.plus(Duration.ofMinutes(5)), EGAL))
                .isEqualTo(Verification.EXPIRE);
        assertThat(code.essais()).isZero();
        assertThat(code.estInutilisableA(EMISSION.plus(Duration.ofMinutes(5)))).isTrue();
        assertThat(code.estInutilisableA(EMISSION.plusSeconds(299))).isFalse();
    }

    @Test
    void au_cinquieme_essai_faux_le_code_est_bloque_meme_si_le_bon_code_suit() {
        CodeConnexion code = code();
        Instant t = EMISSION.plusSeconds(10);

        for (int i = 1; i <= 4; i++) {
            assertThat(code.verifier("000000", t, EGAL)).isEqualTo(Verification.INCORRECT);
            assertThat(code.essaisRestants()).isEqualTo(5 - i);
        }
        assertThat(code.verifier("000000", t, EGAL)).isEqualTo(Verification.ESSAIS_EPUISES);
        assertThat(code.verifier("123456", t, EGAL)).isEqualTo(Verification.ESSAIS_EPUISES);
        assertThat(code.essaisRestants()).isZero();
    }

    @Test
    void le_bon_code_au_cinquieme_essai_est_accepte() {
        CodeConnexion code = code();
        for (int i = 0; i < 4; i++) {
            code.verifier("999999", EMISSION, EGAL);
        }

        assertThat(code.verifier("123456", EMISSION, EGAL)).isEqualTo(Verification.VALIDE);
    }

    @Test
    void des_valeurs_incoherentes_sont_refusees() {
        String id = "22123456";
        assertThatThrownBy(() -> CodeConnexion.emettre(id, "h", EMISSION, Duration.ZERO, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CodeConnexion.emettre(id, "h", EMISSION, Duration.ofMinutes(5), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CodeConnexion.reconstituer(id, "h", EMISSION, EMISSION.plusSeconds(1), 5, 6, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_texte_ne_contient_pas_le_hachage() {
        assertThat(code().toString()).doesNotContain("123456");
    }
}
