package com.seheli.catalogue.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.catalogue.domain.Produit;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.CalculPrixService;
import com.seheli.contrat.application.EcheancierService;
import com.seheli.contrat.application.SimulationService;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class CatalogueClientServiceTest {

    private final ProduitsEnMemoire produits = new ProduitsEnMemoire();
    private final FournisseursEnMemoire fournisseurs = new FournisseursEnMemoire();
    private final CatalogueClientService catalogue = service();

    private CatalogueClientService service() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        CalculPrixService calculPrix = new CalculPrixService(parametres);
        return new CatalogueClientService(produits, fournisseurs, calculPrix,
                new SimulationService(calculPrix, new EcheancierService(parametres)), parametres);
    }

    private Fournisseur fournisseur(String remisePct) {
        Fournisseur f = Fournisseur.nouveau("Fournisseur " + UUID.randomUUID(), new BigDecimal(remisePct));
        fournisseurs.enregistrer(f);
        return f;
    }

    private Produit produit(String reference, String designation, CategorieProduit categorie, Fournisseur f,
            long prixPublic) {
        Produit p = Produit.nouveau(reference, designation, "Marque", categorie, f.id(), Montant.de(prixPublic));
        produits.enregistrer(p);
        return p;
    }

    @Test
    void propose_le_prix_de_vente_et_les_conditions_de_l_exemple_chiffre() {
        Produit p = produit("TEL-001", "Galaxy A35", CategorieProduit.TELEPHONE, fournisseur("8"), 20_000);

        assertThat(catalogue.propose(p.id())).hasValueSatisfying(propose -> {
            assertThat(propose.prixVente()).isEqualTo(Montant.de(20_800));
            assertThat(propose.apportMinimum()).isEqualTo(Montant.de(8_320));
            assertThat(propose.resteAFinancer()).isEqualTo(Montant.de(12_480));
            assertThat(propose.dureeMinMois()).isEqualTo(3);
            assertThat(propose.dureeMaxMois()).isEqualTo(6);
            assertThat(propose.mensualiteIndicative()).isEqualTo(Montant.de(2_080));
        });
    }

    @Test
    void liste_les_produits_disponibles_tries_et_filtres_par_categorie() {
        Fournisseur f = fournisseur("8");
        produit("TEL-002", "Redmi Note", CategorieProduit.TELEPHONE, f, 12_000);
        produit("TEL-001", "Galaxy A35", CategorieProduit.TELEPHONE, f, 20_000);
        produit("ELM-001", "Climatiseur", CategorieProduit.ELECTROMENAGER, f, 24_000);

        assertThat(catalogue.proposes(Optional.empty())).extracting(ProduitPropose::designation)
                .containsExactly("Climatiseur", "Galaxy A35", "Redmi Note");
        assertThat(catalogue.proposes(Optional.of(CategorieProduit.TELEPHONE))).extracting(ProduitPropose::designation)
                .containsExactly("Galaxy A35", "Redmi Note");
    }

    @Test
    void recherche_dans_la_designation_et_la_marque_sans_accents_ni_casse() {
        Fournisseur f = fournisseur("8");
        produit("ELM-001", "Réfrigérateur combiné 300 L", CategorieProduit.ELECTROMENAGER, f, 28_000);
        produit("ELM-002", "Climatiseur split", CategorieProduit.ELECTROMENAGER, f, 24_000);
        produit("TEL-001", "Galaxy A35", CategorieProduit.TELEPHONE, f, 20_000);

        assertThat(catalogue.proposes(Optional.empty(), "REFRIGERATEUR")).extracting(ProduitPropose::designation)
                .containsExactly("Réfrigérateur combiné 300 L");
        assertThat(catalogue.proposes(Optional.empty(), "combine 300")).hasSize(1);
        assertThat(catalogue.proposes(Optional.empty(), "marque")).hasSize(3);
        assertThat(catalogue.proposes(Optional.of(CategorieProduit.TELEPHONE), "climatiseur")).isEmpty();
        assertThat(catalogue.proposes(Optional.empty(), "  ")).hasSize(3);
    }

    @Test
    void la_mensualite_a_partir_de_est_celle_de_la_simulation_sur_la_duree_maximale() {
        Fournisseur f = fournisseur("8");
        // Prix variés : restes divisibles par 6 ou non, arrondi vers le haut ou vers le bas.
        for (long prixPublic : new long[] {20_000, 12_500, 9_500, 7_000, 6_500, 13_333}) {
            Produit p = produit("REF-" + prixPublic, "Produit " + prixPublic, CategorieProduit.TELEPHONE, f,
                    prixPublic);
            ProduitPropose propose = catalogue.propose(p.id()).orElseThrow();
            var simulation = catalogue.simuler(p.id(), 6).orElseThrow();
            assertThat(propose.mensualiteIndicative()).isEqualTo(simulation.mensualite());
            assertThat(propose.prixVente()).isEqualTo(propose.apportMinimum().plus(propose.resteAFinancer()));
        }
    }

    @Test
    void ne_propose_pas_un_produit_indisponible() {
        Produit p = produit("TEL-001", "Galaxy A35", CategorieProduit.TELEPHONE, fournisseur("8"), 20_000);
        p.rendreIndisponible();
        produits.enregistrer(p);

        assertThat(catalogue.propose(p.id())).isEmpty();
        assertThat(catalogue.proposes(Optional.empty())).isEmpty();
    }

    @Test
    void ne_propose_pas_un_produit_dont_le_fournisseur_est_inactif() {
        Fournisseur f = fournisseur("8");
        Produit p = produit("TEL-001", "Galaxy A35", CategorieProduit.TELEPHONE, f, 20_000);
        f.desactiver();
        fournisseurs.enregistrer(f);

        assertThat(catalogue.propose(p.id())).isEmpty();
        assertThat(catalogue.proposes(Optional.empty())).isEmpty();
    }

    @Test
    void ne_propose_pas_un_produit_dont_la_remise_sort_des_bornes_en_vigueur() {
        Produit p = produit("TEL-001", "Galaxy A35", CategorieProduit.TELEPHONE, fournisseur("12"), 20_000);

        assertThat(catalogue.propose(p.id())).isEmpty();
    }

    @Test
    void un_produit_inconnu_n_est_pas_propose() {
        assertThat(catalogue.propose(UUID.randomUUID())).isEmpty();
    }

    @Test
    void simule_un_produit_propose_sur_la_duree_choisie() {
        Produit p = produit("TEL-002", "Galaxy A35", CategorieProduit.TELEPHONE, fournisseur("8"), 20_000);

        assertThat(catalogue.simuler(p.id(), 4)).hasValueSatisfying(s -> {
            assertThat(s.prixTotal()).isEqualTo(Montant.de(20_800));
            assertThat(s.apport()).isEqualTo(Montant.de(8_320));
            assertThat(s.mensualite()).isEqualTo(Montant.de(3_120));
        });
        assertThat(catalogue.simuler(UUID.randomUUID(), 4)).isEmpty();
    }
}
