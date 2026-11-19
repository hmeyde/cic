package com.seheli.catalogue.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.seheli.catalogue.application.CatalogueClientService;
import com.seheli.catalogue.application.FournisseursEnMemoire;
import com.seheli.catalogue.application.ProduitPropose;
import com.seheli.catalogue.application.ProduitsEnMemoire;
import com.seheli.catalogue.application.SimulationClient;
import com.seheli.catalogue.application.SimulationClientService;
import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.catalogue.domain.Produit;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.CalculPrixService;
import com.seheli.contrat.application.EcheancierService;
import com.seheli.contrat.application.SimulationService;
import com.seheli.contrat.domain.SimulationDetaillee;
import com.seheli.identite.application.ClientsEnMemoire;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.application.EmploisEnMemoire;
import com.seheli.identite.domain.Emploi;
import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeContrat;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class CatalogueClientControllerTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private final Clock clock = Clock.fixed(LocalDate.of(2026, 11, 19).atTime(10, 0).atZone(NOUAKCHOTT).toInstant(),
            NOUAKCHOTT);
    private final ProduitsEnMemoire produits = new ProduitsEnMemoire();
    private final FournisseursEnMemoire fournisseurs = new FournisseursEnMemoire();
    private final EmploisEnMemoire emplois = new EmploisEnMemoire();
    private final UUID clientId = UUID.randomUUID();
    private final Principal principal = clientId::toString;
    private final CatalogueClientController controller = controller();
    private final Fournisseur fournisseur = Fournisseur.nouveau("Mauritel", new BigDecimal("8"));
    private final Produit telephone = Produit.nouveau("TEL-001", "Galaxy A35", "Samsung",
            CategorieProduit.TELEPHONE, fournisseur.id(), Montant.de(20_000));
    private final Produit climatiseur = Produit.nouveau("ELM-001", "Climatiseur", "Gree",
            CategorieProduit.ELECTROMENAGER, fournisseur.id(), Montant.de(24_000));

    CatalogueClientControllerTest() {
        fournisseurs.enregistrer(fournisseur);
        produits.enregistrer(telephone);
        produits.enregistrer(climatiseur);
    }

    private CatalogueClientController controller() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        CalculPrixService calculPrix = new CalculPrixService(parametres);
        SimulationService simulation = new SimulationService(calculPrix, new EcheancierService(parametres));
        CatalogueClientService catalogue = new CatalogueClientService(produits, fournisseurs, calculPrix, simulation,
                parametres);
        return new CatalogueClientController(catalogue, new SimulationClientService(catalogue, simulation,
                new EmploiClientService(new ClientsEnMemoire(), emplois, clock), parametres, clock));
    }

    @SuppressWarnings("unchecked")
    private static List<ProduitPropose> produitsAffiches(ExtendedModelMap model) {
        return (List<ProduitPropose>) model.get("produits");
    }

    @Test
    void affiche_tout_le_catalogue_sans_filtre() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.liste(null, null, null, null, model, new MockHttpServletResponse())).isEqualTo("client/catalogue/liste");
        assertThat(produitsAffiches(model)).extracting(ProduitPropose::designation)
                .containsExactly("Climatiseur", "Galaxy A35");
        assertThat(model.get("categorieChoisie")).isEqualTo("");
    }

    @Test
    void filtre_par_categorie() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.liste("TELEPHONE", null, null, null, model, new MockHttpServletResponse());

        assertThat(produitsAffiches(model)).extracting(ProduitPropose::designation).containsExactly("Galaxy A35");
        assertThat(model.get("categorieChoisie")).isEqualTo("TELEPHONE");
    }

    @Test
    void ignore_une_categorie_inconnue() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.liste("VOITURE", null, null, null, model, new MockHttpServletResponse());

        assertThat(produitsAffiches(model)).hasSize(2);
        assertThat(model.get("categorieChoisie")).isEqualTo("");
    }

    @Test
    void recherche_sans_tenir_compte_des_accents_ni_de_la_casse() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.liste(null, "  samsung ", null, null, model, new MockHttpServletResponse());

        assertThat(produitsAffiches(model)).extracting(ProduitPropose::designation).containsExactly("Galaxy A35");
        assertThat(model.get("recherche")).isEqualTo("samsung");
    }

    @Test
    void une_requete_htmx_ne_recoit_que_les_resultats_sauf_restauration_d_historique() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(controller.liste(null, "gal", "true", null, new ExtendedModelMap(), response))
                .isEqualTo("client/catalogue/liste :: resultats");
        assertThat(response.getHeader("Vary")).isEqualTo("HX-Request");
        assertThat(controller.liste(null, "gal", "true", "true", new ExtendedModelMap(),
                new MockHttpServletResponse())).isEqualTo("client/catalogue/liste");
    }

    @Test
    @SuppressWarnings("unchecked")
    void chaque_produit_affiche_a_une_illustration() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.liste(null, null, null, null, model, new MockHttpServletResponse());

        assertThat((java.util.Map<UUID, String>) model.get("illustrations"))
                .containsEntry(telephone.id(), "/img/produits/telephone.svg")
                .containsEntry(climatiseur.id(), "/img/produits/climatiseur.svg");
    }

    @Test
    void affiche_la_page_d_un_produit_avec_son_prix_de_vente() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.produit(telephone.id(), null, null, principal, model)).isEqualTo("client/catalogue/produit");
        assertThat(((ProduitPropose) model.get("produit")).prixVente()).isEqualTo(Montant.de(20_800));
    }

    @Test
    void la_mensualite_a_partir_de_est_celle_de_la_simulation_sur_six_mois() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.produit(telephone.id(), null, null, principal, model);

        ProduitPropose produit = (ProduitPropose) model.get("produit");
        assertThat(produit.mensualiteIndicative()).isEqualTo(Montant.de(2_080))
                .isEqualTo(simulationAffichee(model).detail().mensualite());
    }

    @Test
    void un_produit_indisponible_ou_inconnu_repond_404() {
        climatiseur.rendreIndisponible();
        produits.enregistrer(climatiseur);

        for (UUID id : List.of(climatiseur.id(), UUID.randomUUID())) {
            assertThatThrownBy(() -> controller.produit(id, null, null, principal, new ExtendedModelMap()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Test
    void la_page_produit_simule_par_defaut_sur_la_duree_maximale_avec_l_apport_minimum() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.produit(telephone.id(), null, null, principal, model);

        SimulationClient simulation = simulationAffichee(model);
        assertThat(simulation.detail().nombreMois()).isEqualTo(6);
        assertThat(simulation.detail().mensualite()).isEqualTo(Montant.de(2_080));
        assertThat(simulation.apportPct()).isEqualTo(40);
        assertThat(simulation.apportPctMin()).isEqualTo(40);
        assertThat(simulation.apportPctMax()).isEqualTo(80);
        assertThat(model.get("durees")).isEqualTo(List.of(3, 4, 5, 6));
    }

    @Test
    void la_page_produit_retient_la_duree_demandee_ou_la_maximale_si_hors_bornes() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.produit(telephone.id(), 4, null, principal, model);
        assertThat(simulationAffichee(model).detail().nombreMois()).isEqualTo(4);

        ExtendedModelMap horsBornes = new ExtendedModelMap();
        controller.produit(telephone.id(), 12, null, principal, horsBornes);
        assertThat(simulationAffichee(horsBornes).detail().nombreMois()).isEqualTo(6);
    }

    @Test
    void le_fragment_htmx_recalcule_la_simulation_pour_la_duree_choisie() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.simulation(telephone.id(), 3, null, principal, model))
                .isEqualTo("client/catalogue/simulation :: reponse");
        SimulationDetaillee d = simulationAffichee(model).detail();
        assertThat(d.prixTotal()).isEqualTo(Montant.de(20_800));
        assertThat(d.apport()).isEqualTo(Montant.de(8_320));
        assertThat(d.mensualite()).isEqualTo(Montant.de(4_160));
    }

    @Test
    void le_fragment_recalcule_les_mensualites_pour_l_apport_choisi_au_curseur() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.simulation(telephone.id(), 6, 50, principal, model);

        SimulationClient simulation = simulationAffichee(model);
        assertThat(simulation.apportPct()).isEqualTo(50);
        assertThat(simulation.detail().apport()).isEqualTo(Montant.de(10_400));
        assertThat(simulation.detail().echeances()).extracting(e -> e.montant())
                .containsOnly(Montant.de(1_733), Montant.de(1_735));
        assertThat(simulation.detail().resteAFinancer()).isEqualTo(Montant.de(10_400));
    }

    @Test
    void un_apport_hors_bornes_est_ramene_dans_les_bornes_du_curseur() {
        ExtendedModelMap bas = new ExtendedModelMap();
        controller.simulation(telephone.id(), 6, 10, principal, bas);
        assertThat(simulationAffichee(bas).apportPct()).isEqualTo(40);
        assertThat(simulationAffichee(bas).detail().apport()).isEqualTo(Montant.de(8_320));

        ExtendedModelMap haut = new ExtendedModelMap();
        controller.simulation(telephone.id(), 6, 100, principal, haut);
        assertThat(simulationAffichee(haut).apportPct()).isEqualTo(80);
        assertThat(simulationAffichee(haut).detail().apport()).isEqualTo(Montant.de(16_640));
    }

    @Test
    void l_echeancier_est_date_au_jour_de_paie_du_client_et_decompose_le_prix() {
        declarerEmploi(Montant.de(30_000), 25);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.simulation(telephone.id(), 6, null, principal, model);

        SimulationDetaillee d = simulationAffichee(model).detail();
        assertThat(d.datesConnues()).isTrue();
        assertThat(d.echeances()).extracting(e -> e.date()).containsExactly(LocalDate.of(2026, 11, 25),
                LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 25), LocalDate.of(2027, 2, 25),
                LocalDate.of(2027, 3, 25), LocalDate.of(2027, 4, 25));
        assertThat(d.prixPublic()).isEqualTo(Montant.de(20_000));
        assertThat(d.margeSeheli()).isEqualTo(Montant.de(800));
        assertThat(d.frais()).isEqualTo(Montant.ZERO);
        assertThat(d.endettementDepasse()).isFalse();
    }

    @Test
    void sans_emploi_declare_les_echeances_ne_sont_pas_datees() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.simulation(telephone.id(), 6, null, principal, model);

        SimulationDetaillee d = simulationAffichee(model).detail();
        assertThat(d.datesConnues()).isFalse();
        assertThat(d.echeances()).allSatisfy(e -> assertThat(e.date()).isNull());
        assertThat(d.endettementDepasse()).isFalse();
    }

    @Test
    void avertit_si_la_mensualite_depasse_le_tiers_du_salaire() {
        declarerEmploi(Montant.de(12_000), 25);
        ExtendedModelMap court = new ExtendedModelMap();
        controller.simulation(telephone.id(), 3, null, principal, court);
        assertThat(simulationAffichee(court).detail().endettementDepasse()).isTrue();
        assertThat(simulationAffichee(court).detail().plafondMensualite()).isEqualTo(Montant.de(4_000));

        ExtendedModelMap long_ = new ExtendedModelMap();
        controller.simulation(telephone.id(), 6, null, principal, long_);
        assertThat(simulationAffichee(long_).detail().endettementDepasse()).isFalse();
    }

    @Test
    void le_fragment_refuse_une_duree_hors_bornes_et_un_produit_inconnu() {
        assertThatThrownBy(() -> controller.simulation(telephone.id(), 7, null, principal, new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> controller.simulation(UUID.randomUUID(), 6, null, principal, new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static SimulationClient simulationAffichee(ExtendedModelMap model) {
        return (SimulationClient) model.get("simulation");
    }

    private void declarerEmploi(Montant salaire, int jourPaie) {
        emplois.enregistrer(Emploi.nouveau(clientId, "SNIM", TypeContrat.CDI, LocalDate.of(2020, 1, 1), null,
                salaire, jourPaie, true, false, ModeRemboursement.RETENUE_EMPLOYEUR));
    }
}
