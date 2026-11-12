package com.cic.contrat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;

class AutorisationPrelevementTest {

    private static final Instant LE = Instant.parse("2026-11-13T09:30:00Z");

    private static Contrat contrat() {
        return Contrat.etablir("CIC-2026-000001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320),
                Echeancier.planifier(Montant.de(12_480), 6, LocalDate.of(2026, 11, 20), 25, Optional.empty()), LE);
    }

    @Test
    void emise_avec_le_contrat_en_virement_permanent_a_signer() {
        Contrat c = contrat();

        AutorisationPrelevement a = AutorisationPrelevement.emettre(c, LE);

        assertThat(a.contratId()).isEqualTo(c.id());
        assertThat(a.demandeId()).isEqualTo(c.demandeId());
        assertThat(a.clientId()).isEqualTo(c.clientId());
        assertThat(a.type()).isEqualTo(TypeAutorisation.VIREMENT_PERMANENT);
        assertThat(a.statut()).isEqualTo(StatutAutorisation.A_SIGNER);
        assertThat(a.emiseLe()).isEqualTo(LE);
    }

    @Test
    void le_contenu_imprime_associe_l_autorisation_a_son_propre_contrat() {
        Contrat c = contrat();
        AutorisationPrelevement autreContrat = AutorisationPrelevement.emettre(contrat(), LE);
        Acheteur acheteur = new Acheteur("Ould Ahmed", "Mohamed", "1234567890", LocalDate.of(1990, 5, 1), "22123456");
        CompteBeneficiaire cic = new CompteBeneficiaire("CIC SARL", "BMCI", "00020");

        assertThatThrownBy(() -> new ContenuAutorisation(autreContrat, c, acheteur, cic, Optional.empty(),
                LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 20)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
