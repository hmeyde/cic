package com.cic.identite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;

class EmploiTest {

    private static final UUID CLIENT = UUID.randomUUID();
    private static final LocalDate DEBUT = LocalDate.of(2023, 1, 1);

    @Test
    void un_cdi_n_a_pas_de_date_de_fin() {
        Emploi e = Emploi.nouveau(CLIENT, " SNIM ", TypeContrat.CDI, DEBUT, null, Montant.de(30_000), 25, true, true,
                ModeRemboursement.DOMICILIATION_BMCI);

        assertThat(e.employeur()).isEqualTo("SNIM");
        assertThat(e.dateFin()).isEmpty();
        assertThat(e.estEnCoursAu(LocalDate.of(2026, 11, 11))).isTrue();
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "SNIM", TypeContrat.CDI, DEBUT, DEBUT.plusYears(1),
                Montant.de(30_000), 25, true, false, ModeRemboursement.RETENUE_EMPLOYEUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_cdd_a_une_date_de_fin_posterieure_au_debut() {
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "A", TypeContrat.CDD, DEBUT, null, Montant.de(20_000), 1,
                true, false, ModeRemboursement.RETENUE_EMPLOYEUR)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "A", TypeContrat.CDD, DEBUT, DEBUT, Montant.de(20_000), 1,
                true, false, ModeRemboursement.RETENUE_EMPLOYEUR)).isInstanceOf(IllegalArgumentException.class);

        Emploi e = Emploi.nouveau(CLIENT, "A", TypeContrat.CDD, DEBUT, LocalDate.of(2026, 12, 31),
                Montant.de(20_000), 1, true, false, ModeRemboursement.VIREMENT_PERMANENT);
        assertThat(e.estEnCoursAu(LocalDate.of(2026, 12, 31))).isTrue();
        assertThat(e.estEnCoursAu(LocalDate.of(2027, 1, 1))).isFalse();
    }

    @Test
    void le_salaire_est_positif_et_le_jour_de_paie_entre_1_et_31() {
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "A", TypeContrat.CDI, DEBUT, null, Montant.ZERO, 1, true,
                false, ModeRemboursement.RETENUE_EMPLOYEUR)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "A", TypeContrat.CDI, DEBUT, null, Montant.de(1), 0, true,
                false, ModeRemboursement.RETENUE_EMPLOYEUR)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "A", TypeContrat.CDI, DEBUT, null, Montant.de(1), 32, true,
                false, ModeRemboursement.RETENUE_EMPLOYEUR)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_salaire_domicilie_a_la_bmci_est_verse_sur_un_compte() {
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "A", TypeContrat.CDI, DEBUT, null, Montant.de(1), 1, false,
                true, ModeRemboursement.RETENUE_EMPLOYEUR)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_mode_de_remboursement_suit_le_versement_du_salaire() {
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "A", TypeContrat.CDI, DEBUT, null, Montant.de(1), 1, true,
                false, ModeRemboursement.DOMICILIATION_BMCI)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "A", TypeContrat.CDI, DEBUT, null, Montant.de(1), 1, false,
                false, ModeRemboursement.VIREMENT_PERMANENT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Emploi.nouveau(CLIENT, "A", TypeContrat.CDI, DEBUT, null, Montant.de(1), 1, false,
                false, null)).isInstanceOf(NullPointerException.class);

        Emploi e = Emploi.nouveau(CLIENT, "A", TypeContrat.CDI, DEBUT, null, Montant.de(1), 1, false, false,
                ModeRemboursement.RETENUE_EMPLOYEUR);
        assertThat(e.modeRemboursement()).isEqualTo(ModeRemboursement.RETENUE_EMPLOYEUR);
    }

    @Test
    void modifier_remplace_les_informations_d_emploi() {
        Emploi e = Emploi.nouveau(CLIENT, "A", TypeContrat.CDI, DEBUT, null, Montant.de(30_000), 25, true, false,
                ModeRemboursement.VIREMENT_PERMANENT);

        e.modifier("B", TypeContrat.CDD, DEBUT, LocalDate.of(2027, 6, 30), Montant.de(40_000), 28, true, true,
                ModeRemboursement.DOMICILIATION_BMCI);

        assertThat(e.employeur()).isEqualTo("B");
        assertThat(e.dateFin()).contains(LocalDate.of(2027, 6, 30));
        assertThat(e.salaireNet()).isEqualTo(Montant.de(40_000));
        assertThat(e.jourPaie()).isEqualTo(28);
        assertThat(e.modeRemboursement()).isEqualTo(ModeRemboursement.DOMICILIATION_BMCI);
        assertThatThrownBy(() -> e.modifier("B", TypeContrat.CDI, DEBUT, null, Montant.de(1), 1, false, false,
                ModeRemboursement.VIREMENT_PERMANENT)).isInstanceOf(IllegalArgumentException.class);
        assertThat(e.employeur()).isEqualTo("B");
    }
}
