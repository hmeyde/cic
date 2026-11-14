package com.seheli.catalogue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FournisseurTest {

    @Test
    void un_nouveau_fournisseur_est_actif_avec_sa_remise_negociee() {
        Fournisseur f = Fournisseur.nouveau("  Mauritel Distribution ", new BigDecimal("8"));

        assertThat(f.id()).isNotNull();
        assertThat(f.nom()).isEqualTo("Mauritel Distribution");
        assertThat(f.remiseNegocieePct()).isEqualByComparingTo("8");
        assertThat(f.estActif()).isTrue();
    }

    @Test
    void la_remise_doit_etre_un_taux_valide() {
        assertThatThrownBy(() -> Fournisseur.nouveau("A", new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Fournisseur.nouveau("A", new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Fournisseur.nouveau("A", new BigDecimal("7.125")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Fournisseur.nouveau("A", new BigDecimal("7.50")).remiseNegocieePct()).isEqualByComparingTo("7.5");
    }

    @Test
    void le_nom_est_obligatoire() {
        assertThatThrownBy(() -> Fournisseur.nouveau("   ", BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renegocie_la_remise_et_change_d_etat() {
        Fournisseur f = Fournisseur.nouveau("A", new BigDecimal("5"));

        f.negocierRemise(new BigDecimal("9"));
        f.desactiver();

        assertThat(f.remiseNegocieePct()).isEqualByComparingTo("9");
        assertThat(f.estActif()).isFalse();
        f.activer();
        assertThat(f.estActif()).isTrue();
    }

    @Test
    void l_identite_repose_sur_l_identifiant() {
        Fournisseur f = Fournisseur.nouveau("A", BigDecimal.TEN);
        Fournisseur meme = Fournisseur.reconstituer(f.id(), "Autre nom", new BigDecimal("5"), false);

        assertThat(meme).isEqualTo(f).hasSameHashCodeAs(f);
    }
}
