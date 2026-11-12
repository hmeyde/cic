package com.cic.demande.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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
import com.cic.contrat.domain.EndettementDepasseException;
import com.cic.contrat.domain.FinContratDepasseeException;
import com.cic.demande.StatutDossierChange;
import com.cic.demande.domain.ConditionEliminatoire;
import com.cic.demande.domain.CritereScore;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandesFixtures;
import com.cic.demande.domain.Recommandation;
import com.cic.demande.domain.StatutDossier;
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

class DemandeServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 12);

    private final Clock clock = Clock.fixed(AUJOURD_HUI.atTime(10, 0).atZone(NOUAKCHOTT).toInstant(), NOUAKCHOTT);
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final EmploisEnMemoire emplois = new EmploisEnMemoire();
    private final ProduitsEnMemoire produits = new ProduitsEnMemoire();
    private final FournisseursEnMemoire fournisseurs = new FournisseursEnMemoire();
    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final List<Object> evenements = new ArrayList<>();
    private final DemandeService service;

    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Mohamed", LocalDate.of(1990, 5, 1),
            "22123456", clock.instant());
    private final Fournisseur fournisseur = Fournisseur.nouveau("Mauritel", new BigDecimal("8"));
    private final Produit telephone = Produit.nouveau("TEL-001", "Galaxy A35", "Samsung", CategorieProduit.TELEPHONE,
            fournisseur.id(), Montant.de(20_000));

    DemandeServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        CalculPrixService calculPrix = new CalculPrixService(parametres);
        EcheancierService echeancier = new EcheancierService(parametres);
        CatalogueClientService catalogue = new CatalogueClientService(produits, fournisseurs, calculPrix,
                new SimulationService(calculPrix, echeancier), parametres);
        ScoreService score = new ScoreService(parametres, new EligibiliteService(parametres, clock), clock);
        service = new DemandeService(demandes, new ClientService(clients),
                new EmploiClientService(clients, emplois, clock), catalogue, echeancier, score, parametres,
                evenements::add, clock);

        client.verifierNni();
        client.verifierPieceIdentite();
        clients.enregistrer(client);
        fournisseurs.enregistrer(fournisseur);
        produits.enregistrer(telephone);
        declarerEmploi(TypeContrat.CDI, null, Montant.de(30_000));
    }

    private void declarerEmploi(TypeContrat type, LocalDate fin, Montant salaire) {
        emplois.enregistrer(Emploi.nouveau(client.id(), "SNIM", type, LocalDate.of(2020, 1, 1), fin, salaire, 25,
                true, false, ModeRemboursement.RETENUE_EMPLOYEUR));
    }

    private Demande soumettre() {
        return service.soumettre(client.id(), telephone.id(), 6, Optional.empty());
    }

    @Test
    void la_demande_soumise_est_enregistree_en_analyse_avec_son_score() {
        Demande demande = soumettre();

        assertThat(demande.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
        assertThat(demandes.parClient(client.id())).containsExactly(demande);
        assertThat(demandes.verrous()).isEqualTo(1);

        var termes = demande.termes();
        assertThat(termes.produitId()).isEqualTo(telephone.id());
        assertThat(termes.fournisseurId()).isEqualTo(fournisseur.id());
        assertThat(termes.designationProduit()).isEqualTo("Galaxy A35");
        assertThat(termes.prixAchat()).isEqualTo(Montant.de(18_400));
        assertThat(termes.prixVente()).isEqualTo(Montant.de(20_800));
        assertThat(termes.apport()).isEqualTo(Montant.de(8_320));
        assertThat(termes.nombreMois()).isEqualTo(6);
        assertThat(termes.mensualite()).isEqualTo(Montant.de(2_080));
        assertThat(termes.derniereMensualite()).isEqualTo(Montant.de(2_080));

        // CDI 25 + plus de 2 ans 15 + retenue employeur 15 + 6,9 % du salaire 15 + apport minimum 0.
        assertThat(demande.score().eligibilite().estEligible()).isTrue();
        assertThat(demande.score().note().note()).isEqualTo(70);
        assertThat(demande.score().recommandation()).isEqualTo(Recommandation.FAVORABLE);
        assertThat(demande.score().versionGrille()).isEqualTo("v1");
    }

    @Test
    void chaque_statut_est_publie_pour_le_journal_d_audit() {
        Demande demande = soumettre();

        assertThat(evenements).containsExactly(
                new StatutDossierChange(demande.id(), client.id(), null, StatutDossier.DEMANDE_SOUMISE, clock.instant()),
                new StatutDossierChange(demande.id(), client.id(), StatutDossier.DEMANDE_SOUMISE,
                        StatutDossier.EN_ANALYSE, clock.instant()));
    }

    @Test
    void un_seul_dossier_actif_par_client() {
        soumettre();

        assertThatThrownBy(this::soumettre).isInstanceOf(DossierActifExistantException.class);
        assertThat(demandes.parClient(client.id())).hasSize(1);
    }

    @Test
    void un_dossier_en_recouvrement_bloque_les_nouveaux_achats() {
        demandes.enregistrer(DemandesFixtures.demande(client.id(), StatutDossier.RECOUVREMENT));

        assertThatThrownBy(this::soumettre).isInstanceOf(DossierActifExistantException.class);
    }

    @Test
    void nouvelle_demande_apres_un_dossier_solde_avec_historique() {
        demandes.enregistrer(DemandesFixtures.demande(client.id(), StatutDossier.SOLDEE));
        demandes.enregistrer(DemandesFixtures.demande(client.id(), StatutDossier.REFUSEE));

        Demande demande = soumettre();

        assertThat(demande.score().note().resultat(CritereScore.HISTORIQUE_CIC).points()).isEqualTo(5);
        assertThat(demandes.parClient(client.id())).hasSize(3);
    }

    @Test
    void regle_du_tiers_du_salaire() {
        // 2 080 > 6 000 / 3 = 2 000.
        declarerEmploi(TypeContrat.CDI, null, Montant.de(6_000));

        assertThatThrownBy(this::soumettre).isInstanceOf(EndettementDepasseException.class);
        assertThat(demandes.parClient(client.id())).isEmpty();
        assertThat(evenements).isEmpty();
    }

    @Test
    void tiers_du_salaire_atteint_exactement() {
        declarerEmploi(TypeContrat.CDI, null, Montant.de(6_240));

        assertThat(soumettre().statut()).isEqualTo(StatutDossier.EN_ANALYSE);
    }

    @Test
    void cdd_termine_avant_la_derniere_echeance() {
        declarerEmploi(TypeContrat.CDD, LocalDate.of(2027, 3, 1), Montant.de(30_000));

        assertThatThrownBy(this::soumettre).isInstanceOf(FinContratDepasseeException.class);
    }

    @Test
    void apport_superieur_reduit_les_mensualites_et_releve_le_score() {
        Demande demande = service.soumettre(client.id(), telephone.id(), 6, Optional.of(Montant.de(14_560)));

        assertThat(demande.termes().apport()).isEqualTo(Montant.de(14_560));
        assertThat(demande.termes().mensualite()).isEqualTo(Montant.de(1_040));
        assertThat(demande.score().note().resultat(CritereScore.APPORT_SUPPLEMENTAIRE).points()).isEqualTo(5);
    }

    @Test
    void apport_inferieur_au_minimum_ou_couvrant_tout_le_prix() {
        assertThatThrownBy(() -> service.soumettre(client.id(), telephone.id(), 6, Optional.of(Montant.de(8_000))))
                .isInstanceOf(ApportInvalideException.class);
        assertThatThrownBy(() -> service.soumettre(client.id(), telephone.id(), 6, Optional.of(Montant.de(20_800))))
                .isInstanceOf(ApportInvalideException.class);
        assertThatThrownBy(() -> service.soumettre(client.id(), telephone.id(), 6, Optional.of(Montant.de(20_797))))
                .isInstanceOf(ApportInvalideException.class);
    }

    @Test
    void duree_hors_bornes() {
        assertThatThrownBy(() -> service.soumettre(client.id(), telephone.id(), 12, Optional.empty()))
                .isInstanceOf(DureeInvalideException.class);
    }

    @Test
    void emploi_non_declare() {
        Client autre = Client.nouveau("1234567891", "Sy", "Aminata", LocalDate.of(1995, 1, 1), "22123457",
                clock.instant());
        clients.enregistrer(autre);

        assertThatThrownBy(() -> service.soumettre(autre.id(), telephone.id(), 6, Optional.empty()))
                .isInstanceOf(EmploiNonDeclareException.class);
    }

    @Test
    void produit_indisponible() {
        telephone.rendreIndisponible();
        produits.enregistrer(telephone);

        assertThatThrownBy(this::soumettre).isInstanceOf(ProduitNonProposeException.class);
        assertThatThrownBy(() -> service.soumettre(client.id(), UUID.randomUUID(), 6, Optional.empty()))
                .isInstanceOf(ProduitNonProposeException.class);
    }

    @Test
    void identite_non_verifiee_soumise_mais_defavorable() {
        Client nouveau = Client.nouveau("1234567892", "Ba", "Oumar", LocalDate.of(1988, 1, 1), "22123458",
                clock.instant());
        clients.enregistrer(nouveau);
        emplois.enregistrer(Emploi.nouveau(nouveau.id(), "SNIM", TypeContrat.FONCTIONNAIRE, LocalDate.of(2015, 1, 1),
                null, Montant.de(40_000), 28, true, true, ModeRemboursement.DOMICILIATION_BMCI));

        Demande demande = service.soumettre(nouveau.id(), telephone.id(), 3, Optional.empty());

        assertThat(demande.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
        assertThat(demande.score().eligibilite().resultat(ConditionEliminatoire.IDENTITE_VERIFIEE).satisfaite())
                .isFalse();
        assertThat(demande.score().recommandation()).isEqualTo(Recommandation.DEFAVORABLE);
    }
}
