package com.seheli.catalogue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;

class ProduitTest {

    private static final UUID FOURNISSEUR = UUID.randomUUID();

    private static Produit exemple() {
        return Produit.nouveau("TEL-A55", "Galaxy A55 128 Go", "Samsung", CategorieProduit.TELEPHONE, FOURNISSEUR,
                Montant.de(20_000));
    }

    @Test
    void un_nouveau_produit_est_disponible() {
        Produit p = exemple();

        assertThat(p.id()).isNotNull();
        assertThat(p.reference()).isEqualTo("TEL-A55");
        assertThat(p.categorie()).isEqualTo(CategorieProduit.TELEPHONE);
        assertThat(p.fournisseurId()).isEqualTo(FOURNISSEUR);
        assertThat(p.prixPublic()).isEqualTo(Montant.de(20_000));
        assertThat(p.estDisponible()).isTrue();
    }

    @Test
    void le_prix_public_doit_etre_positif() {
        assertThatThrownBy(() -> Produit.nouveau("R", "D", "M", CategorieProduit.TELEPHONE, FOURNISSEUR, Montant.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exemple().changerPrixPublic(Montant.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void les_champs_texte_sont_obligatoires() {
        assertThatThrownBy(() -> Produit.nouveau(" ", "D", "M", CategorieProduit.TELEPHONE, FOURNISSEUR,
                Montant.de(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Produit.nouveau("R", "D", "M", null, FOURNISSEUR, Montant.de(1)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void change_de_prix_et_de_disponibilite() {
        Produit p = exemple();

        p.changerPrixPublic(Montant.de(21_000));
        p.rendreIndisponible();

        assertThat(p.prixPublic()).isEqualTo(Montant.de(21_000));
        assertThat(p.estDisponible()).isFalse();
        p.rendreDisponible();
        assertThat(p.estDisponible()).isTrue();
    }
}
