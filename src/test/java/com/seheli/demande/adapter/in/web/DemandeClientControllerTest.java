package com.seheli.demande.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.contrat.application.ContratsEnMemoire;
import com.seheli.contrat.application.AutorisationsEnMemoire;
import com.seheli.contrat.application.ContratService;
import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.catalogue.application.CatalogueClientService;
import com.seheli.catalogue.application.FournisseursEnMemoire;
import com.seheli.catalogue.application.ProduitsEnMemoire;
import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.catalogue.domain.Produit;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.AutorisationPdfService;
import com.seheli.contrat.application.CalculPrixService;
import com.seheli.contrat.application.ContratPdfService;
import com.seheli.contrat.application.EcheancierService;
import com.seheli.contrat.application.SimulationService;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.StatutEcheance;
import com.seheli.demande.application.ApportInvalideException;
import com.seheli.demande.application.DemandeService;
import com.seheli.demande.application.DecisionsDeTest;
import com.seheli.demande.application.DemandesEnMemoire;
import com.seheli.demande.application.DossierActifExistantException;
import com.seheli.demande.application.EligibiliteService;
import com.seheli.demande.application.ScoreService;
import com.seheli.demande.application.RemiseNonConfirmeeException;
import com.seheli.demande.application.RemiseProduitService;
import com.seheli.demande.application.PrealablesCommandeService;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.StatutAutorisation;
import com.seheli.paiement.application.PaiementService;
import com.seheli.paiement.application.PaiementsEnMemoire;
import com.seheli.paiement.application.SaisiePaiement;
import com.seheli.paiement.domain.ReferenceApport;
import com.seheli.paiement.domain.ReferenceEcheance;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.ProchaineEtape;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.demande.domain.SuiviDossier;
import com.seheli.identite.application.ClientService;
import com.seheli.identite.application.ClientsEnMemoire;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.application.EmploisEnMemoire;
import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.Emploi;
import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeContrat;
import com.seheli.paiement.application.InstructionsApportService;
import com.seheli.paiement.domain.InstructionsApport;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

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
    private final PaiementService paiements;
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
                echeancier, new ContratService(new ContratsEnMemoire(), new AutorisationsEnMemoire(), parametres, clock),
                new ScoreService(parametres, new EligibiliteService(parametres, clock), clock),
                parametres, new JournalAuditService(new JournalEnMemoire()), evenement -> { }, clock);
        decisions = new DecisionsDeTest(demandes, new JournalAuditService(new JournalEnMemoire()), emplois,
                evenement -> { }, clock);
        paiements = new PaiementService(new PaiementsEnMemoire(), decisions.contratService,
                new JournalAuditService(new JournalEnMemoire()), evenement -> { }, clock);
        controller = new DemandeClientController(service, decisions.contratService,
                new ContratPdfService(decisions.contrats, new ClientService(clients), parametres,
                        contenu -> ("%PDF " + contenu.contrat().numero()).getBytes(StandardCharsets.US_ASCII), clock),
                new AutorisationPdfService(decisions.contrats, decisions.autorisations, new ClientService(clients),
                        new EmploiClientService(clients, emplois, clock), parametres,
                        contenu -> ("%PDF ordre " + contenu.contrat().numero()).getBytes(StandardCharsets.US_ASCII),
                        clock),
                new InstructionsApportService(parametres),
                new RemiseProduitService(demandes, decisions.contratService,
                        new JournalAuditService(new JournalEnMemoire()), evenement -> { }, clock),
                new PrealablesCommandeService(decisions.contratService, paiements),
                clock);

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
            assertThat(o.numero()).isEqualTo("SEH-2026-000001");
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
    void apres_l_acceptation_le_client_voit_comment_payer_son_apport_par_masrvi() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        decisions.service.accepter(lignes().getFirst().id(), "analyste");
        ExtendedModelMap model = new ExtendedModelMap();

        controller.suivi(principal, model);

        assertThat(model.get("paiementApport")).isInstanceOfSatisfying(InstructionsApport.class, i -> {
            assertThat(i.montant()).isEqualTo(Montant.de(8_320));
            assertThat(i.codeMarchand()).isEqualTo("012345");
            assertThat(i.reference().valeur()).isEqualTo("AP20260000011");
        });
    }

    @Test
    void pas_d_instructions_de_paiement_avant_l_acceptation() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        ExtendedModelMap model = new ExtendedModelMap();

        controller.suivi(principal, model);

        assertThat(model.containsAttribute("paiementApport")).isFalse();
    }

    @Test
    void apres_l_acceptation_le_client_telecharge_son_contrat_en_pdf() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        decisions.service.accepter(lignes().getFirst().id(), "analyste");

        ResponseEntity<byte[]> reponse = controller.contratPdf(principal);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(reponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"contrat-SEH-2026-000001.pdf\"");
        assertThat(reponse.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(new String(reponse.getBody(), StandardCharsets.US_ASCII)).isEqualTo("%PDF SEH-2026-000001");
    }

    @Test
    void apres_l_acceptation_le_client_telecharge_son_autorisation_de_prelevement_remplie() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        decisions.service.accepter(lignes().getFirst().id(), "analyste");

        ResponseEntity<byte[]> reponse = controller.autorisationPdf(principal);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(reponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"autorisation-prelevement-SEH-2026-000001.pdf\"");
        assertThat(reponse.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(new String(reponse.getBody(), StandardCharsets.US_ASCII)).isEqualTo("%PDF ordre SEH-2026-000001");
    }

    @Test
    void pas_d_autorisation_pdf_tant_que_le_dossier_n_est_pas_accepte() {
        assertThat(controller.autorisationPdf(principal).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());

        assertThat(controller.autorisationPdf(principal).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void pas_de_contrat_pdf_tant_que_le_dossier_n_est_pas_accepte() {
        assertThat(controller.contratPdf(principal).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());

        assertThat(controller.contratPdf(principal).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /** Dossier du client accepté puis passé à COMMANDEE (commande fournisseur testée à part). */
    private UUID dossierCommande() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        UUID demandeId = lignes().getFirst().id();
        decisions.service.accepter(demandeId, "analyste");
        Demande d = demandes.parId(demandeId).orElseThrow();
        demandes.enregistrer(Demande.reconstituer(d.id(), d.clientId(), d.termes(), d.score(), d.soumiseLe(),
                StatutDossier.COMMANDEE, clock.instant(), d.decision()));
        return demandeId;
    }

    @Test
    void le_client_confirme_la_remise_et_son_remboursement_demarre() {
        UUID demandeId = dossierCommande();
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.confirmerRemise(principal, "oui", redirection);

        assertThat(vue).isEqualTo("redirect:/client/dossier");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("client.remise.confirmee");
        assertThat(demandes.parId(demandeId).orElseThrow().statut()).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
        ExtendedModelMap model = new ExtendedModelMap();
        controller.suivi(principal, model);
        assertThat(model.get("suivi")).isInstanceOfSatisfying(SuiviDossier.class,
                s -> assertThat(s.prochaine()).isEqualTo(ProchaineEtape.PROVISIONNER_COMPTE));
        assertThat(model.get("offre")).isInstanceOfSatisfying(OffreClient.class,
                o -> assertThat(o.echeances()).extracting(Echeance::statut).containsOnly(StatutEcheance.A_VENIR));
    }

    @Test
    void remise_sans_la_case_cochee_revient_au_suivi_avec_le_message() {
        UUID demandeId = dossierCommande();
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.confirmerRemise(principal, null, redirection);

        assertThat(vue).isEqualTo("redirect:/client/dossier");
        assertThat(redirection.getFlashAttributes().get("erreur")).isEqualTo(RemiseNonConfirmeeException.CODE);
        assertThat(demandes.parId(demandeId).orElseThrow().statut()).isEqualTo(StatutDossier.COMMANDEE);
    }

    @Test
    void remise_impossible_avant_la_commande() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        controller.confirmerRemise(principal, "oui", redirection);

        assertThat(redirection.getFlashAttributes().get("erreur")).isEqualTo("dossier.transition.interdite");
        assertThat(lignes().getFirst().statut()).isEqualTo(StatutDossier.EN_ANALYSE);
    }

    private ProchaineActionClient action() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.suivi(principal, model);
        return (ProchaineActionClient) model.get("action");
    }

    private UUID dossierAccepte() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());
        UUID demandeId = lignes().getFirst().id();
        decisions.service.accepter(demandeId, "analyste");
        return demandeId;
    }

    private void passerA(UUID demandeId, StatutDossier statut) {
        Demande d = demandes.parId(demandeId).orElseThrow();
        demandes.enregistrer(Demande.reconstituer(d.id(), d.clientId(), d.termes(), d.score(), d.soumiseLe(),
                statut, clock.instant(), d.decision()));
    }

    @Test
    void en_analyse_la_prochaine_action_n_a_pas_de_donnees_d_offre() {
        controller.soumettre(principal, telephone.id(), 6, null, new RedirectAttributesModelMap());

        ProchaineActionClient a = action();

        assertThat(a.apport()).isNull();
        assertThat(a.joursRestants()).isNull();
        assertThat(a.documents()).isFalse();
    }

    @Test
    void acceptee_la_prochaine_action_donne_la_reference_masrvi_le_compte_a_rebours_et_les_documents() {
        dossierAccepte();

        ProchaineActionClient a = action();

        assertThat(a.apport().reference().valeur()).isEqualTo("AP20260000011");
        assertThat(a.apport().montant()).isEqualTo(Montant.de(8_320));
        assertThat(a.codeMarchand()).isEqualTo("012345");
        assertThat(a.apportEncaisse()).isFalse();
        assertThat(a.autorisation()).isEqualTo(StatutAutorisation.A_SIGNER);
        assertThat(a.joursRestants()).isEqualTo(7L);
        assertThat(a.delaiEcoule()).isZero();
        assertThat(a.documents()).isTrue();
        assertThat(a.prochaineEcheance()).isNull();
    }

    @Test
    void acceptee_l_apport_recu_est_coche() {
        dossierAccepte();
        Contrat c = decisions.contrats.tous().getFirst();
        paiements.saisir(new SaisiePaiement(ReferenceApport.pourContrat(c.numero()).valeur(), "8320",
                "2026-11-12"), "recouvrement");

        assertThat(action().apportEncaisse()).isTrue();
    }

    @Test
    void annulee_plus_de_documents_ni_d_instructions() {
        passerA(dossierAccepte(), StatutDossier.ANNULEE);

        ProchaineActionClient a = action();

        assertThat(a.apport()).isNull();
        assertThat(a.documents()).isFalse();
    }

    @Test
    void en_remboursement_la_prochaine_mensualite() {
        passerA(dossierAccepte(), StatutDossier.EN_REMBOURSEMENT);

        ProchaineActionClient a = action();

        assertThat(a.apport()).isNull();
        assertThat(a.joursRestants()).isNull();
        assertThat(a.prochaineEcheance().rang()).isEqualTo(1);
        assertThat(a.prochaineEcheance().montant()).isEqualTo(Montant.de(2_080));
        assertThat(a.aRegulariser()).isNull();
    }

    @Test
    void en_retard_l_echeance_a_regulariser_avec_sa_reference() {
        UUID demandeId = dossierAccepte();
        passerA(demandeId, StatutDossier.EN_RETARD);
        Contrat c = decisions.contrats.parDemande(demandeId).orElseThrow();
        for (int rang : List.of(1, 2)) {
            Echeance e = c.echeance(rang).orElseThrow();
            decisions.contrats.enregistrerStatut(c.id(), Echeance.reconstituer(e.id(), e.rang(), e.date(),
                    e.montant(), StatutEcheance.EN_RETARD));
        }

        ProchaineActionClient a = action();

        assertThat(a.prochaineEcheance()).isNull();
        assertThat(a.aRegulariser().rang()).isEqualTo(1);
        assertThat(a.referenceRegularisation()).isEqualTo(ReferenceEcheance.pourEcheance(c.numero(), 1).valeur());
        assertThat(a.totalEnRetard()).isEqualTo(Montant.de(4_160));
        assertThat(a.codeMarchand()).isEqualTo("012345");
    }

    @Test
    void suivi_sans_dossier() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.suivi(principal, model)).isEqualTo("client/dossier/suivi");
        assertThat(model.containsAttribute("suivi")).isFalse();
    }

    private RemboursementClient remboursement() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.remboursement(principal, model)).isEqualTo("client/dossier/remboursement");
        return (RemboursementClient) model.get("remboursement");
    }

    private void marquer(UUID demandeId, StatutEcheance statut, int... rangs) {
        Contrat c = decisions.contrats.parDemande(demandeId).orElseThrow();
        for (int rang : rangs) {
            Echeance e = c.echeance(rang).orElseThrow();
            decisions.contrats.enregistrerStatut(c.id(), Echeance.reconstituer(e.id(), e.rang(), e.date(),
                    e.montant(), statut));
        }
    }

    @Test
    void remboursement_pas_encore_commence_avant_la_remise() {
        dossierAccepte();
        ExtendedModelMap model = new ExtendedModelMap();

        controller.remboursement(principal, model);

        assertThat(model.containsAttribute("suivi")).isTrue();
        assertThat(model.containsAttribute("remboursement")).isFalse();
    }

    @Test
    void remboursement_sans_dossier() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.remboursement(principal, model)).isEqualTo("client/dossier/remboursement");
        assertThat(model.containsAttribute("suivi")).isFalse();
        assertThat(model.containsAttribute("remboursement")).isFalse();
    }

    @Test
    void en_remboursement_deux_sur_six_payees_le_restant_et_la_prochaine_echeance() {
        UUID demandeId = dossierAccepte();
        passerA(demandeId, StatutDossier.EN_REMBOURSEMENT);
        marquer(demandeId, StatutEcheance.PAYEE, 1, 2);

        RemboursementClient r = remboursement();

        assertThat(r.payees()).isEqualTo(2);
        assertThat(r.total()).isEqualTo(6);
        assertThat(r.montantPaye()).isEqualTo(Montant.de(4_160));
        assertThat(r.montantRestant()).isEqualTo(Montant.de(8_320));
        assertThat(r.prochaine().rang()).isEqualTo(3);
        assertThat(r.joursAvantProchaine())
                .isEqualTo(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2026, 11, 12), r.prochaine().date()));
        assertThat(r.enRetard()).isFalse();
        assertThat(r.referenceRegularisation()).isNull();
        assertThat(r.solde()).isFalse();
        assertThat(r.arcs()).extracting(RemboursementClient.Arc::statut).containsExactly(StatutEcheance.PAYEE,
                StatutEcheance.PAYEE, StatutEcheance.A_VENIR, StatutEcheance.A_VENIR, StatutEcheance.A_VENIR,
                StatutEcheance.A_VENIR);
    }

    @Test
    void en_retard_l_echeance_a_regulariser_sans_penalite() {
        UUID demandeId = dossierAccepte();
        passerA(demandeId, StatutDossier.EN_RETARD);
        marquer(demandeId, StatutEcheance.PAYEE, 1, 2);
        marquer(demandeId, StatutEcheance.EN_RETARD, 3);
        Contrat c = decisions.contrats.parDemande(demandeId).orElseThrow();

        RemboursementClient r = remboursement();

        assertThat(r.enRetard()).isTrue();
        assertThat(r.prochaine().rang()).isEqualTo(3);
        assertThat(r.prochaine().montant()).isEqualTo(Montant.de(2_080));
        assertThat(r.totalEnRetard()).isEqualTo(Montant.de(2_080));
        assertThat(r.montantRestant()).isEqualTo(Montant.de(8_320));
        assertThat(r.referenceRegularisation()).isEqualTo(ReferenceEcheance.pourEcheance(c.numero(), 3).valeur());
        assertThat(r.codeMarchand()).isEqualTo("012345");
    }

    @Test
    void soldee_six_sur_six_rien_a_payer() {
        UUID demandeId = dossierAccepte();
        passerA(demandeId, StatutDossier.SOLDEE);
        marquer(demandeId, StatutEcheance.PAYEE, 1, 2, 3, 4, 5, 6);

        RemboursementClient r = remboursement();

        assertThat(r.solde()).isTrue();
        assertThat(r.pourcentage()).isEqualTo(100);
        assertThat(r.montantRestant()).isEqualTo(Montant.ZERO);
        assertThat(r.montantPaye()).isEqualTo(Montant.de(12_480));
        assertThat(r.prochaine()).isNull();
        assertThat(r.joursAvantProchaine()).isNull();
        assertThat(r.enRetard()).isFalse();
    }

    @Test
    void anneau_un_arc_par_echeance_separes_par_un_ecart() {
        Contrat c = decisions.contrats.parDemande(dossierAccepte()).orElseThrow();

        List<RemboursementClient.Arc> arcs = RemboursementClient.arcs(c.echeances());

        double pas = 2 * Math.PI * RemboursementClient.RAYON / 6;
        assertThat(arcs).hasSize(6);
        assertThat(arcs.getFirst().dashoffset()).isEqualTo("0");
        String[] dash = arcs.getFirst().dasharray().split(" ");
        assertThat(Double.parseDouble(dash[0])).isCloseTo(pas - 3, org.assertj.core.data.Offset.offset(0.01));
        assertThat(Double.parseDouble(dash[0]) + Double.parseDouble(dash[1]))
                .isCloseTo(6 * pas, org.assertj.core.data.Offset.offset(0.02));
        assertThat(Double.parseDouble(arcs.get(1).dashoffset())).isCloseTo(-pas, org.assertj.core.data.Offset.offset(0.01));
        assertThat(RemboursementClient.arcs(c.echeances().subList(0, 1)).getFirst().dasharray())
                .startsWith("326.73");
    }
}
