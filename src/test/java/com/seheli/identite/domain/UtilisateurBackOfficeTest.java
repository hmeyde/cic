package com.seheli.identite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UtilisateurBackOfficeTest {

    @Test
    void un_nouveau_compte_est_actif_et_son_identifiant_est_normalise() {
        UtilisateurBackOffice u = UtilisateurBackOffice.nouveau("  Fatima.Ba ", "$2a$hash", RoleBackOffice.ANALYSTE);

        assertThat(u.identifiant()).isEqualTo("fatima.ba");
        assertThat(u.role()).isEqualTo(RoleBackOffice.ANALYSTE);
        assertThat(u.estActif()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "nom avec espace", "élève", "a/b"})
    void un_identifiant_invalide_est_refuse(String identifiant) {
        assertThatThrownBy(() -> UtilisateurBackOffice.nouveau(identifiant, "$2a$hash", RoleBackOffice.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_identifiant_de_plus_de_50_caracteres_est_refuse() {
        assertThatThrownBy(() -> UtilisateurBackOffice.normaliser("a".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(UtilisateurBackOffice.normaliser("a".repeat(50))).hasSize(50);
    }

    @Test
    void le_hachage_est_obligatoire() {
        assertThatThrownBy(() -> UtilisateurBackOffice.nouveau("admin", " ", RoleBackOffice.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_compte_peut_etre_desactive_puis_reactive() {
        UtilisateurBackOffice u = UtilisateurBackOffice.nouveau("admin", "$2a$hash", RoleBackOffice.ADMIN);

        u.desactiver();
        assertThat(u.estActif()).isFalse();
        u.activer();
        assertThat(u.estActif()).isTrue();
    }

    @Test
    void la_representation_texte_n_expose_ni_le_hachage_ni_l_identifiant() {
        UtilisateurBackOffice u = UtilisateurBackOffice.nouveau("admin", "$2a$secret", RoleBackOffice.ADMIN);

        assertThat(u.toString()).doesNotContain("$2a$secret").doesNotContain("admin");
    }
}
