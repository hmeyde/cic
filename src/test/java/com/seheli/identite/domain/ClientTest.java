package com.seheli.identite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ClientTest {

    private static final Instant CONSENTEMENT = Instant.parse("2026-11-01T10:00:00Z");

    private static Client client() {
        return Client.nouveau("1234 567 890", " Ould Ahmed ", "Sidi", LocalDate.of(1990, 5, 12), "+222 36 12 34 56",
                CONSENTEMENT);
    }

    @Test
    void un_nouveau_client_a_son_nni_et_son_telephone_normalises_et_une_identite_non_verifiee() {
        Client c = client();

        assertThat(c.id()).isNotNull();
        assertThat(c.nni()).isEqualTo("1234567890");
        assertThat(c.telephone()).isEqualTo("36123456");
        assertThat(c.nom()).isEqualTo("Ould Ahmed");
        assertThat(c.consentementDonneesLe()).isEqualTo(CONSENTEMENT);
        assertThat(c.estNniVerifie()).isFalse();
        assertThat(c.estPieceIdentiteVerifiee()).isFalse();
    }

    @Test
    void le_nni_doit_contenir_dix_chiffres() {
        assertThatThrownBy(() -> Client.normaliserNni("123456789")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Client.normaliserNni("12345678AB")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_telephone_doit_contenir_huit_chiffres() {
        assertThat(Client.normaliserTelephone("0022222334455")).isEqualTo("22334455");
        assertThatThrownBy(() -> Client.normaliserTelephone("2233445")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_consentement_est_obligatoire() {
        assertThatThrownBy(() -> Client.nouveau("1234567890", "A", "B", LocalDate.of(1990, 1, 1), "22334455", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void corriger_l_identite_annule_les_verifications() {
        Client c = client();
        c.verifierNni();
        c.verifierPieceIdentite();

        c.corrigerIdentite("Ould Mohamed", "Sidi", LocalDate.of(1990, 5, 12));

        assertThat(c.estNniVerifie()).isFalse();
        assertThat(c.estPieceIdentiteVerifiee()).isFalse();
    }

    @Test
    void l_age_est_calcule_en_annees_revolues() {
        Client c = client();
        assertThat(c.ageAu(LocalDate.of(2008, 5, 11))).isEqualTo(17);
        assertThat(c.ageAu(LocalDate.of(2008, 5, 12))).isEqualTo(18);
    }

    @Test
    void le_texte_du_client_ne_contient_aucune_donnee_personnelle() {
        Client c = client();
        assertThat(c.toString()).doesNotContain(c.nni(), c.telephone(), c.nom());
    }
}
