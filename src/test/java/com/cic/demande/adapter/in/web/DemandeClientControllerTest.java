package com.cic.demande.adapter.in.web;

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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.catalogue.application.CatalogueClientService;
import com.cic.catalogue.application.FournisseursEnMemoire;
import com.cic.catalogue.application.ProduitsEnMemoire;
import com.cic.catalogue.domain.CategorieProduit;
import com.cic.catalogue.domain.Fournisseur;
import com.cic.catalogue.domain.Produit;
import com.cic.commun.domain.Montant;
import com.cic.contrat.application.CalculPrixService;
import com.cic.contrat.application.EcheancierService;
import com.cic.contrat.application.SimulationService;
import com.cic.contrat.domain.Echeance;
import com.cic.contrat.domain.StatutEcheance;
import com.cic.demande.application.ApportInvalideException;
import com.cic.demande.application.DemandeService;
import com.cic.demande.application.DecisionsDeTest;
import com.cic.demande.application.DemandesEnMemoire;
import com.cic.demande.application.DossierActifExistantException;
import com.cic.demande.application.EligibiliteService;
import com.cic.demande.application.ScoreService;
import com.cic.demande.domain.ProchaineEtape;
import com.cic.demande.domain.StatutDossier;
import com.cic.demande.domain.SuiviDossier;
import com.cic.identite.application.ClientService;
import com.cic.identite.application.ClientsEnMemoire;
import com.cic.identite.application.EmploiClientService;
import com.cic.identite.application.EmploisEnMemoire;
import com.cic.identite.domain.Client;
import com.cic.identite.domain.Emploi;
import com.cic.identite.domain.ModeRemboursement;
import com.cic.identite.domain.TypeContrat;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class DemandeClientControllerTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");

    private final Clock clock = Clock.fixed(LocalDate.of(2026, 11, 12).atTime(10, 0).atZone(NOUAKCHOTT).toInstant(),
            NOUAKCHOTT);
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final EmploisEnMemoire emplois = new EmploisEnMemoire();
    private final ProduitsEnMemoire produits = new ProduitsEnMemoire();
    private final FournisseursEnMemoire fournisseurs = new FournisseursEnMemoire();
    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Mohamed", LocalDate.of(1990, 5, 1),
            "22123456", clock.instant());
    private final Principal principal = () -> client.id().toString();
    private final Fournisseur fournisseur = Fournisseur.nouveau("Mauritel", new BigDecimal("8"));
    private final Produit telephone = Produit.nouveau("TEL-001", "Galaxy A35", "Samsung", CategorieProduit.TELEPHONE,
            fournisseur.id(), Montant.de(20_000));
    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final DecisionsDeTest decisions;
    private final DemandeClientController controller;

    DemandeClientControllerTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        CalculPrixService calculPrix = new CalculPrixService(parametres);
        EcheancierService echeancier = new EcheancierService(parametres);
        DemandeService service = new DemandeService(demandes, new ClientService(clients),
                new EmploiClientService(clients, emplois, clock),
                new CatalogueClientService(produits, fournisseurs, calculPrix,
                        new SimulationService(calculPrix, echeancier), parametres),
                echeancier, new ScoreService(parametres, new EligibiliteService(parametres, clock), clock),
                parametres, new JournalAuditService(new JournalEnMemoire()), evenement -> { }, clock);
        decisions = new DecisionsDeTest(demandes, new JournalAuditService(new JournalEnMemoire()), emplois,
                evenement -> { }, clock);
        controller = new DemandeClientController(service, decisions.contratService, clock);

        clients.enregistrer(client);
        fournisseurs.enregistrer(fournisseur);
        produits.enregistrer(telephone);
        emplois.enregistrer(Emploi.nouveau(client.id(), "SNIM", TypeContrat.CDI, LocalDate.of(2020, 1, 1), null,
                Montant.de(30_000), 25, true, false, ModeRemboursement.RETENUE_EMPLOYEUR));
    }

    @SuppressWarnings("unchecked")
    private List<LigneDemande> lignes() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.liste(principal, model)).isEqualTo("client/demandes/liste");
        return (List<LigneDemande>) model.get("demandes");
    }

    @Test
    void le_client_soumet_puis_voit_sa_demande() {
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.soumettre(principal, telephone.id(), 6, "", redirection);

        assertThat(vue).isEqualTo("redirect:/client/dossier");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("client.demande.soumise");
        assertThat(lignes()).singleElement().satisfies(l -> {
            assertThat(l.designation()).isEqualTo("Galaxy A35");
            assertThat(l.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
            assertThat(l.soumiseLe()).isEqualTo(LocalDate.of(2026, 11, 12));
            assertThat(l.prixVente()).isEqualTo(Montant.de(20_800));
            assertThat(l.mensualite()).isEqualTo(Montant.de(2_080));
            assertThat(l.derniereDiffere()).isFalse();
        });
    }

    @Test
    void regle_non_respectee_ramene_sur_le_produit_avec_le_message() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.soumettre(principal, telephone.id(), 4, null, redirection);

        assertThat(vue).isEqualTo("redirect:/client/catalogue/" + telephone.id() + "?duree=4");
        assertThat(redirection.getFlashAttributes().get("erreur")).isEqualTo(DossierActifExistantException.CODE);
        assertThat(lignes()).hasSize(1);
    }

    @Test
    void aucune_demande() {
        assertThat(lignes()).isEmpty();
    }

    @Test
    void lecture_de_l_apport() {
        assertThat(DemandeClientController.lireApport(null)).isEmpty();
        assertThat(DemandeClientController.lireApport("  ")).isEmpty();
        assertThat(DemandeClientController.lireApport("10 000")).contains(Montant.de(10_000));
        assertThat(DemandeClientController.lireApport("10 000")).contains(Montant.de(10_000));
        assertThatThrownBy(() -> DemandeClientController.lireApport("10000,5"))
                .isInstanceOf(ApportInvalideException.class);
        assertThatThrownBy(() -> DemandeClientController.lireApport("-5")).isInstanceOf(ApportInvalideException.class);
        assertThatThrownBy(() -> DemandeClientController.lireApport("abc")).isInstanceOf(ApportInvalideException.class);
    }

    @Test
    void apport_illisible_ramene_sur_le_produit() {
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        controller.soumettre(principal, telephone.id(), 6, "abc", redirection);

        assertThat(redirection.getFlashAttributes().get("erreur")).isEqualTo(ApportInvalideException.CODE);
        assertThat(lignes()).isEmpty();
    }

    @Test
    void le_client_suit_son_dossier() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.suivi(principal, model)).isEqualTo("client/dossier/suivi");

        assertThat(model.get("suivi")).isInstanceOfSatisfying(SuiviDossier.class, s -> {
            assertThat(s.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
            assertThat(s.prochaine()).isEqualTo(ProchaineEtape.ATTENDRE_DECISION);
        });
        assertThat(model.get("demande")).isInstanceOfSatisfying(LigneDemande.class,
                l -> assertThat(l.designation()).isEqualTo("Galaxy A35"));
    }

    @Test
    void pas_d_offre_tant_que_le_dossier_n_est_pas_accepte() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        ExtendedModelMap model = new ExtendedModelMap();

        controller.suivi(principal, model);

        assertThat(model.containsAttribute("offre")).isFalse();
    }

    @Test
    void apres_l_acceptation_le_client_voit_son_offre_et_son_echeancier() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        UUID demandeId = lignes().getFirst().id();
        decisions.service.accepter(demandeId, "analyste");
        ExtendedModelMap model = new ExtendedModelMap();

        controller.suivi(principal, model);

        assertThat(model.get("suivi")).isInstanceOfSatisfying(SuiviDossier.class,
                s -> assertThat(s.statut()).isEqualTo(StatutDossier.ACCEPTEE));
        assertThat(model.get("offre")).isInstanceOfSatisfying(OffreClient.class, o -> {
            assertThat(o.numero()).isEqualTo("CIC-2026-000001");
            assertThat(o.designation()).isEqualTo("Galaxy A35");
            assertThat(o.etablieLe()).isEqualTo(LocalDate.of(2026, 11, 12));
            assertThat(o.prixTotal()).isEqualTo(Montant.de(20_800));
            assertThat(o.apport()).isEqualTo(Montant.de(8_320));
            assertThat(o.resteAFinancer()).isEqualTo(Montant.de(12_480));
            assertThat(o.nombreMois()).isEqualTo(6);
            assertThat(o.echeances()).extracting(Echeance::date).containsExactly(LocalDate.of(2026, 11, 25),
                    LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 25), LocalDate.of(2027, 2, 25),
                    LocalDate.of(2027, 3, 25), LocalDate.of(2027, 4, 25));
            assertThat(o.echeances()).extracting(Echeance::montant).containsOnly(Montant.de(2_080));
            assertThat(o.echeances()).extracting(Echeance::statut).containsOnly(StatutEcheance.A_VENIR);
        });
    }

    @Test
    void suivi_sans_dossier() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.suivi(principal, model)).isEqualTo("client/dossier/suivi");
        assertThat(model.containsAttribute("suivi")).isFalse();
    }
}
