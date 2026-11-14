package com.seheli.paiement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;

class PaiementTest {

    private static final ReferencePaiement REFERENCE = ReferencePaiement.lire("AP20260000011");
    private static final LocalDate LE = LocalDate.of(2026, 11, 12);
    private static final Instant SAISI = Instant.parse("2026-11-12T10:00:00Z");
    private static final Rattachement APPORT = Rattachement.apport(UUID.randomUUID(), "SEH-2026-000001",
            UUID.randomUUID(), UUID.randomUUID(), Montant.de(8_320));

    @Test
    void rattache_au_dossier_de_sa_reference() {
        Paiement p = Paiement.saisir(REFERENCE, Montant.de(8_320), LE, Optional.of(APPORT), "admin", SAISI);

        assertThat(p.statut()).isEqualTo(StatutRapprochement.RAPPROCHE);
        assertThat(p.rattachement()).contains(APPORT);
        assertThat(p.ecartDeMontant()).isFalse();
    }

    @Test
    void ecart_si_le_montant_differe_de_l_apport() {
        Paiement p = Paiement.saisir(REFERENCE, Montant.de(8_000), LE, Optional.of(APPORT), "admin", SAISI);

        assertThat(p.estRapproche()).isTrue();
        assertThat(p.ecartDeMontant()).isTrue();
    }

    @Test
    void non_rapproche_sans_dossier() {
        Paiement p = Paiement.saisir(REFERENCE, Montant.de(8_320), LE, Optional.empty(), "admin", SAISI);

        assertThat(p.statut()).isEqualTo(StatutRapprochement.NON_RAPPROCHE);
        assertThat(p.ecartDeMontant()).isFalse();
    }

    @Test
    void montant_nul_refuse() {
        assertThatThrownBy(() -> Paiement.saisir(REFERENCE, Montant.ZERO, LE, Optional.empty(), "admin", SAISI))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
