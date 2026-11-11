package com.cic.identite.adapter.out.code;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GenerateursCodeTest {

    @Test
    void le_code_aleatoire_fait_toujours_6_chiffres_et_varie() {
        GenerateurCodeAleatoire generateur = new GenerateurCodeAleatoire();
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generateur.generer();
            assertThat(code).matches("\\d{6}");
            codes.add(code);
        }
        assertThat(codes).hasSizeGreaterThan(900);
    }

    @Test
    void le_code_de_demonstration_est_000000() {
        assertThat(new GenerateurCodeFixe().generer()).isEqualTo("000000");
    }
}
