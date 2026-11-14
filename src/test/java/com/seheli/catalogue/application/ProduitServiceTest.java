package com.seheli.catalogue.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.catalogue.domain.Produit;
import com.seheli.commun.domain.Montant;

class ProduitServiceTest {

    private final ProduitsEnMemoire produits = new ProduitsEnMemoire();
    private final FournisseursEnMemoire fournisseurs = new FournisseursEnMemoire();
    private final ProduitService service = new ProduitService(produits, fournisseurs);

    private Fournisseur fournisseur(String nom, boolean actif) {
        Fournisseur f = Fournisseur.nouveau(nom, new BigDecimal("8"));
        if (!actif) {
            f.desactiver();
        }
        fournisseurs.enregistrer(f);
        return f;
    }

    private Produit telephone(Fournisseur f) {
        return service.creer("TEL-001", "Galaxy A15", "Samsung", CategorieProduit.TELEPHONE, f.id(),
                Montant.de(20_000));
    }

    @Test
    void cree_un_produit_disponible_et_l_enregistre() {
        Fournisseur f = fournisseur("Mauritel", true);

        Produit cree = service.creer(" TEL-001 ", " Galaxy A15 ", "Samsung", CategorieProduit.TELEPHONE, f.id(),
                Montant.de(20_000));

        Produit relu = service.parId(cree.id());
        assertThat(relu.reference()).isEqualTo("TEL-001");
        assertThat(relu.designation()).isEqualTo("Galaxy A15");
        assertThat(relu.prixPublic()).isEqualTo(Montant.de(20_000));
        assertThat(relu.fournisseurId()).isEqualTo(f.id());
        assertThat(relu.estDisponible()).isTrue();
        assertThat(service.lister()).extracting(Produit::id).containsExactly(cree.id());
    }

    @Test
    void la_reference_est_unique() {
        Fournisseur f = fournisseur("Mauritel", true);
        telephone(f);

        assertThatThrownBy(() -> service.creer("TEL-001", "Autre", "Tecno", CategorieProduit.TELEPHONE, f.id(),
                Montant.de(5_000)))
                .isInstanceOf(ReferenceProduitDejaUtiliseeException.class);
        assertThat(produits.tous()).hasSize(1);
    }

    @Test
    void le_fournisseur_doit_exister_et_etre_actif() {
        Fournisseur inactif = fournisseur("Ancien", false);

        assertThatThrownBy(() -> telephone(inactif)).isInstanceOf(FournisseurInactifException.class);
        assertThatThrownBy(() -> service.creer("TEL-002", "X", "Y", CategorieProduit.TELEPHONE, UUID.randomUUID(),
                Montant.de(1_000)))
                .isInstanceOf(FournisseurIntrouvableException.class);
        assertThat(produits.tous()).isEmpty();
    }

    @Test
    void modifie_la_description_le_prix_le_fournisseur_et_la_disponibilite() {
        Fournisseur premier = fournisseur("Mauritel", true);
        Fournisseur second = fournisseur("Chinguitel", true);
        Produit p = telephone(premier);

        service.modifier(p.id(), "Réfrigérateur 300 L", "LG", CategorieProduit.ELECTROMENAGER, second.id(),
                Montant.de(35_000), false);

        Produit relu = service.parId(p.id());
        assertThat(relu.reference()).isEqualTo("TEL-001");
        assertThat(relu.designation()).isEqualTo("Réfrigérateur 300 L");
        assertThat(relu.marque()).isEqualTo("LG");
        assertThat(relu.categorie()).isEqualTo(CategorieProduit.ELECTROMENAGER);
        assertThat(relu.fournisseurId()).isEqualTo(second.id());
        assertThat(relu.prixPublic()).isEqualTo(Montant.de(35_000));
        assertThat(relu.estDisponible()).isFalse();
    }

    @Test
    void un_produit_dont_le_fournisseur_est_desactive_reste_modifiable() {
        Fournisseur f = fournisseur("Mauritel", true);
        Produit p = telephone(f);
        f.desactiver();
        fournisseurs.enregistrer(f);

        service.modifier(p.id(), "Galaxy A15 128 Go", "Samsung", CategorieProduit.TELEPHONE, f.id(),
                Montant.de(21_000), false);

        assertThat(service.parId(p.id()).designation()).isEqualTo("Galaxy A15 128 Go");
    }

    @Test
    void on_ne_peut_pas_passer_a_un_fournisseur_inactif() {
        Produit p = telephone(fournisseur("Mauritel", true));
        Fournisseur inactif = fournisseur("Ancien", false);

        assertThatThrownBy(() -> service.modifier(p.id(), "Galaxy A15", "Samsung", CategorieProduit.TELEPHONE,
                inactif.id(), Montant.de(20_000), true))
                .isInstanceOf(FournisseurInactifException.class);
    }

    @Test
    void rend_un_produit_indisponible_puis_disponible() {
        Produit p = telephone(fournisseur("Mauritel", true));

        service.changerDisponibilite(p.id(), false);
        assertThat(produits.disponibles()).isEmpty();

        service.changerDisponibilite(p.id(), true);
        assertThat(produits.disponibles()).extracting(Produit::id).containsExactly(p.id());
    }

    @Test
    void un_produit_inconnu_est_introuvable() {
        UUID inconnu = UUID.randomUUID();

        assertThatThrownBy(() -> service.parId(inconnu)).isInstanceOf(ProduitIntrouvableException.class);
        assertThatThrownBy(() -> service.changerDisponibilite(inconnu, false))
                .isInstanceOf(ProduitIntrouvableException.class);
    }
}
