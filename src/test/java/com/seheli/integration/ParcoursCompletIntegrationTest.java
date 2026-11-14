package com.seheli.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.domain.EntreeJournal;
import com.seheli.audit.domain.ObjetAudite;
import com.seheli.catalogue.application.FournisseurService;
import com.seheli.catalogue.application.ProduitService;
import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.commun.domain.Montant;
import com.seheli.comptabilite.application.ComptabiliteService;
import com.seheli.comptabilite.domain.Compte;
import com.seheli.comptabilite.domain.EcritureComptable;
import com.seheli.comptabilite.domain.LigneEcriture;
import com.seheli.comptabilite.domain.NatureEcriture;
import com.seheli.comptabilite.domain.Sens;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.SuiviAutorisationService;
import com.seheli.contrat.domain.AutorisationPrelevement;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.StatutAutorisation;
import com.seheli.contrat.domain.StatutEcheance;
import com.seheli.demande.application.DemandeService;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.identite.application.ClientService;
import com.seheli.identite.domain.TypeDocument;
import com.seheli.inscription.application.InscriptionService;
import com.seheli.paiement.domain.ReferenceApport;
import com.seheli.paiement.domain.ReferenceEcheance;

/**
 * Parcours complet de bout en bout, par HTTP (MockMvc, sécurité et CSRF actifs) sur PostgreSQL : de la
 * pré-inscription publique jusqu'au dossier SOLDEE, en vérifiant à chaque étape les montants de
 * l'exemple chiffré de CLAUDE.md et les écritures comptables passées.
 *
 * <p>Prix public 20 000 MRU, remise fournisseur 8 % → prix d'achat 18 400 ; marge 4 % → prix de
 * vente 20 800 ; apport 40 % = 8 320 ; reste 12 480 = 6 × 2 080 ; capital engagé 10 080 ; bénéfice
 * brut 2 400. Une fois le dossier soldé, la banque a reçu 8 320 + 12 480 et versé 18 400 : son solde
 * sur le dossier est le bénéfice brut, et les comptes clients sont à zéro.
 *
 * <p>Seule étape hors HTTP, comme dans {@link ParcoursDemandeIntegrationTest} : la vérification de
 * l'identité, sans écran au back-office ({@link ClientService#verifierIdentite}). Les écritures sont
 * passées par des écouteurs d'événements après la transaction : le test les attend.
 */
class ParcoursCompletIntegrationTest extends TestIntegration {

    private static final byte[] PDF = "%PDF-1.4\n% document de test\n".getBytes(StandardCharsets.US_ASCII);
    private static final Duration DELAI_EVENEMENTS = Duration.ofSeconds(10);

    @Autowired
    private WebApplicationContext contexte;
    @Autowired
    private Clock clock;
    @Autowired
    private FournisseurService fournisseurs;
    @Autowired
    private ProduitService produits;
    @Autowired
    private InscriptionService inscriptions;
    @Autowired
    private ClientService clients;
    @Autowired
    private DemandeService demandes;
    @Autowired
    private ContratService contrats;
    @Autowired
    private SuiviAutorisationService autorisations;
    @Autowired
    private ComptabiliteService comptabilite;
    @Autowired
    private JournalAuditService journal;

    private MockMvc mvc;
    private UUID produitId;
    private UUID clientId;

    @BeforeEach
    void preparer() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        UUID fournisseurId = fournisseurs.creer("Fournisseur parcours " + suffixe, new BigDecimal("8")).id();
        produitId = produits.creer("PC-" + suffixe, "Téléphone parcours", "Marque", CategorieProduit.TELEPHONE,
                fournisseurId, Montant.de(20_000)).id();
    }

    @Test
    void deLInscriptionJusquAuDossierSolde() throws Exception {
        String telephone = telephoneUnique();

        // 1. Pré-inscription publique (anonyme).
        long inscritsAvant = inscriptions.nombre();
        mvc.perform(post("/inscription").with(csrf())
                        .param("nom", "Client Parcours").param("telephone", telephone)
                        .param("employeur", "Employeur parcours").param("typeContrat", "CDI")
                        .param("trancheSalaire", "PLUS_DE_40000").param("produitSouhaite", "Téléphone")
                        .param("consentement", "on"))
                .andExpect(redirectedUrl("/inscription/merci"));
        assertThat(inscriptions.nombre()).isEqualTo(inscritsAvant + 1);

        // 2. Compte client par code à usage unique, emploi, justificatifs, identité vérifiée.
        MockHttpSession client = creerCompte(telephone);
        clientId = clients.parTelephone(telephone).orElseThrow().id();
        declarerEmploiCdi(client);
        envoyerJustificatif(client, TypeDocument.PIECE_IDENTITE);
        envoyerJustificatif(client, TypeDocument.BULLETIN_PAIE);
        clients.verifierIdentite(clientId);

        // 3. Simulation puis demande sur 6 mois.
        mvc.perform(get("/client/catalogue/{id}/simulation", produitId).session(client).param("duree", "6"))
                .andExpect(status().isOk());
        mvc.perform(post("/client/demandes").session(client).with(csrf())
                        .param("produit", produitId.toString()).param("duree", "6"))
                .andExpect(redirectedUrl("/client/dossier"));
        Demande demande = seuleDemande(clientId);
        UUID demandeId = demande.id();
        assertThat(demande.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
        assertThat(demande.termes().prixVente()).isEqualTo(Montant.de(20_800));
        assertThat(demande.termes().prixAchat()).isEqualTo(Montant.de(18_400));
        assertThat(demande.termes().apport()).isEqualTo(Montant.de(8_320));
        assertThat(demande.termes().mensualite()).isEqualTo(Montant.de(2_080));

        // 4. Acceptation par l'analyste : contrat, échéancier et autorisation à signer.
        MockHttpSession analyste = connecterBackOffice("analyste", "analyste-dev");
        mvc.perform(post("/bo/dossiers/{id}/acceptation", demandeId).session(analyste).with(csrf()))
                .andExpect(flash().attribute("succes", "bo.decision.acceptee"));
        assertThat(statut(demandeId)).isEqualTo(StatutDossier.ACCEPTEE);

        Contrat contrat = contrats.parDemande(demandeId).orElseThrow();
        assertThat(contrat.prix().prixPublic()).isEqualTo(Montant.de(20_000));
        assertThat(contrat.prix().prixAchat()).isEqualTo(Montant.de(18_400));
        assertThat(contrat.prix().prixVente()).isEqualTo(Montant.de(20_800));
        assertThat(contrat.apport()).isEqualTo(Montant.de(8_320));
        assertThat(contrat.resteAFinancer()).isEqualTo(Montant.de(12_480));
        assertThat(contrat.capitalEngage()).isEqualTo(Montant.de(10_080));
        assertThat(contrat.beneficeBrut()).isEqualTo(Montant.de(2_400));
        assertThat(contrat.echeances()).hasSize(6)
                .allSatisfy(e -> {
                    assertThat(e.montant()).isEqualTo(Montant.de(2_080));
                    assertThat(e.statut()).isEqualTo(StatutEcheance.A_VENIR);
                    assertThat(e.date().getDayOfMonth()).isEqualTo(25);
                });
        assertThat(contrat.echeances().stream().map(Echeance::montant).reduce(Montant.ZERO, Montant::plus))
                .isEqualTo(Montant.de(12_480));

        mvc.perform(get("/client/dossier").session(client)).andExpect(status().isOk());
        mvc.perform(get("/client/dossier/contrat.pdf").session(client))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        mvc.perform(get("/client/dossier/autorisation-prelevement.pdf").session(client))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // 5. Apport payé par référence Masrvi, saisi au back-office ; une seconde saisie est refusée.
        MockHttpSession recouvrement = connecterBackOffice("recouvrement", "recouvrement-dev");
        String referenceApport = ReferenceApport.pourContrat(contrat.numero()).valeur();
        saisirPaiement(recouvrement, referenceApport, "8320");
        attendre(() -> ecrituresDe(demandeId, NatureEcriture.APPORT_ENCAISSE) == 1);
        mvc.perform(post("/bo/paiements").session(recouvrement).with(csrf())
                        .param("reference", referenceApport).param("montant", "8320")
                        .param("recuLe", LocalDate.now(clock).toString()))
                .andExpect(redirectedUrl("/bo/paiements"))
                .andExpect(flash().attributeExists("erreur"));
        // Apport seul : l'autorisation n'est pas encore acceptée.
        assertThat(statut(demandeId)).isEqualTo(StatutDossier.ACCEPTEE);

        // 6. Autorisation de prélèvement : signée, transmise, acceptée par la banque.
        UUID autorisationId = autorisations.parDemande(demandeId).orElseThrow().id();
        String ficheAutorisation = "/bo/autorisations/" + autorisationId;
        mvc.perform(multipart(ficheAutorisation + "/document-signe")
                        .file(new MockMultipartFile("fichier", "autorisation.pdf", "application/pdf", PDF))
                        .session(analyste).with(csrf()))
                .andExpect(redirectedUrl(ficheAutorisation))
                .andExpect(flash().attribute("succes", "bo.autorisation.document.depose"));
        mvc.perform(post(ficheAutorisation + "/transmission").session(analyste).with(csrf()))
                .andExpect(flash().attribute("succes", "bo.autorisation.transmise"));
        mvc.perform(post(ficheAutorisation + "/acceptation").session(analyste).with(csrf()))
                .andExpect(flash().attribute("succes", "bo.autorisation.acceptee"));
        assertThat(autorisations.parDemande(demandeId)).map(AutorisationPrelevement::statut)
                .contains(StatutAutorisation.ACCEPTEE);
        attendre(() -> statut(demandeId) == StatutDossier.APPORT_ET_AUTORISATION);

        // 7. Commande fournisseur : l'analyste qui a décidé ne peut pas la valider, l'administrateur oui.
        mvc.perform(post("/bo/commandes/{id}/validation", demandeId).session(analyste).with(csrf()))
                .andExpect(redirectedUrl("/bo/commandes"))
                .andExpect(flash().attributeExists("erreur"));
        assertThat(statut(demandeId)).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
        MockHttpSession admin = connecterBackOffice("admin", "admin-dev");
        mvc.perform(post("/bo/commandes/{id}/validation", demandeId).session(admin).with(csrf()))
                .andExpect(redirectedUrl("/bo/commandes"))
                .andExpect(flash().attribute("succes", "bo.commandes.validee"));
        assertThat(statut(demandeId)).isEqualTo(StatutDossier.COMMANDEE);
        mvc.perform(get("/bo/commandes/{id}/bon-de-commande.pdf", demandeId).session(admin))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        attendre(() -> ecrituresDe(demandeId, NatureEcriture.PAIEMENT_FOURNISSEUR) == 1);

        // 8. Le client confirme la remise : LIVREE puis EN_REMBOURSEMENT, vente comptabilisée.
        mvc.perform(post("/client/dossier/remise").session(client).with(csrf()).param("confirmation", "on"))
                .andExpect(redirectedUrl("/client/dossier"))
                .andExpect(flash().attribute("succes", "client.remise.confirmee"));
        assertThat(statut(demandeId)).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
        attendre(() -> ecrituresDe(demandeId, NatureEcriture.VENTE_LIVREE) == 1);

        // 9. Six échéances prélevées par leur référence ; solde refusé tant qu'il en reste une.
        String ficheRecouvrement = "/bo/recouvrement/dossiers/" + demandeId;
        for (int rang = 1; rang <= 6; rang++) {
            if (rang == 6) {
                mvc.perform(post(ficheRecouvrement + "/solde").session(recouvrement).with(csrf()))
                        .andExpect(redirectedUrl(ficheRecouvrement))
                        .andExpect(flash().attributeExists("erreur"));
                assertThat(statut(demandeId)).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
            }
            saisirPaiement(recouvrement, ReferenceEcheance.pourEcheance(contrat.numero(), rang).valeur(), "2080");
        }
        assertThat(contrats.parDemande(demandeId).orElseThrow().echeances())
                .extracting(Echeance::statut).containsOnly(StatutEcheance.PAYEE);
        attendre(() -> ecrituresDe(demandeId, NatureEcriture.ECHEANCE_ENCAISSEE) == 6);

        // 10. Le recouvrement solde le dossier.
        mvc.perform(post(ficheRecouvrement + "/solde").session(recouvrement).with(csrf()))
                .andExpect(redirectedUrl(ficheRecouvrement))
                .andExpect(flash().attribute("succes", "bo.recouvrement.solde"));
        assertThat(statut(demandeId)).isEqualTo(StatutDossier.SOLDEE);
        mvc.perform(get("/client/dossier").session(client)).andExpect(status().isOk());

        // Journal du dossier : chaque transition, dans l'ordre.
        assertThat(journal.historique(new ObjetAudite("DEMANDE", demandeId)).stream().map(EntreeJournal::action))
                .containsExactly("SOUMISSION", "MISE_EN_ANALYSE", "ACCEPTATION", "APPORT_ET_AUTORISATION_RECUS",
                        "COMMANDE_FOURNISSEUR", "CONFIRMATION_REMISE", "DEBUT_REMBOURSEMENT", "SOLDE");

        verifierEcritures(demandeId, contrat);
    }

    /** Écritures du dossier : une par fait, équilibrées, montants de l'exemple chiffré, soldes finaux. */
    private void verifierEcritures(UUID demandeId, Contrat contrat) {
        List<EcritureComptable> ecritures = comptabilite.duDossier(demandeId);
        assertThat(ecritures).extracting(EcritureComptable::nature).containsExactlyInAnyOrder(
                NatureEcriture.APPORT_ENCAISSE, NatureEcriture.PAIEMENT_FOURNISSEUR, NatureEcriture.VENTE_LIVREE,
                NatureEcriture.ECHEANCE_ENCAISSEE, NatureEcriture.ECHEANCE_ENCAISSEE,
                NatureEcriture.ECHEANCE_ENCAISSEE, NatureEcriture.ECHEANCE_ENCAISSEE,
                NatureEcriture.ECHEANCE_ENCAISSEE, NatureEcriture.ECHEANCE_ENCAISSEE);
        assertThat(ecritures).allSatisfy(e -> {
            assertThat(e.total(Sens.DEBIT)).isEqualTo(e.total(Sens.CREDIT));
            assertThat(e.contratId()).contains(contrat.id());
            assertThat(e.estContrepassation()).isFalse();
        });

        assertThat(lignes(ecritures, NatureEcriture.APPORT_ENCAISSE)).containsExactlyInAnyOrder(
                LigneEcriture.debit(Compte.BANQUE, Montant.de(8_320)),
                LigneEcriture.credit(Compte.CLIENTS_APPORTS, Montant.de(8_320)));
        assertThat(lignes(ecritures, NatureEcriture.PAIEMENT_FOURNISSEUR)).containsExactlyInAnyOrder(
                LigneEcriture.debit(Compte.ACHATS, Montant.de(18_400)),
                LigneEcriture.credit(Compte.BANQUE, Montant.de(18_400)));
        assertThat(lignes(ecritures, NatureEcriture.VENTE_LIVREE)).containsExactlyInAnyOrder(
                LigneEcriture.debit(Compte.CLIENTS_APPORTS, Montant.de(8_320)),
                LigneEcriture.debit(Compte.CLIENTS, Montant.de(12_480)),
                LigneEcriture.credit(Compte.VENTES, Montant.de(20_800)));
        assertThat(ecritures.stream().filter(e -> e.nature() == NatureEcriture.ECHEANCE_ENCAISSEE))
                .allSatisfy(e -> assertThat(e.lignes()).containsExactlyInAnyOrder(
                        LigneEcriture.debit(Compte.BANQUE, Montant.de(2_080)),
                        LigneEcriture.credit(Compte.CLIENTS, Montant.de(2_080))));

        // Soldes (débit − crédit) du dossier soldé.
        Map<Compte, BigDecimal> soldes = soldes(ecritures);
        assertThat(soldes.get(Compte.BANQUE)).isEqualByComparingTo(contrat.beneficeBrut().valeur());
        assertThat(soldes.get(Compte.BANQUE)).isEqualByComparingTo("2400");
        assertThat(soldes.get(Compte.CLIENTS)).isEqualByComparingTo("0");
        assertThat(soldes.get(Compte.CLIENTS_APPORTS)).isEqualByComparingTo("0");
        assertThat(soldes.get(Compte.ACHATS)).isEqualByComparingTo("18400");
        assertThat(soldes.get(Compte.VENTES)).isEqualByComparingTo("-20800");
        // Marge réalisée = ventes − achats = bénéfice brut.
        assertThat(soldes.get(Compte.VENTES).negate().subtract(soldes.get(Compte.ACHATS)))
                .isEqualByComparingTo("2400");
    }

    // --- Étapes du parcours -------------------------------------------------------------------

    private MockHttpSession creerCompte(String telephone) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/client/connexion").session(session).with(csrf()).param("telephone", telephone))
                .andExpect(redirectedUrl("/client/connexion/code"));
        mvc.perform(post("/client/connexion/code").session(session).with(csrf())
                        .param("code", CodeConnexionFixe.CODE))
                .andExpect(redirectedUrl("/client/connexion/compte"));
        mvc.perform(post("/client/connexion/compte").session(session).with(csrf())
                        .param("nni", nniUnique()).param("nom", "Parcours").param("prenom", "Client")
                        .param("dateNaissance", LocalDate.now(clock).minusYears(30).toString())
                        .param("consentement", "true"))
                .andExpect(redirectedUrl("/client/"))
                .andExpect(flash().attribute("succes", "client.compte.cree"));
        return session;
    }

    /** CDI depuis 5 ans, 50 000 MRU nets payés le 25, remboursement par retenue de l'employeur. */
    private void declarerEmploiCdi(MockHttpSession client) throws Exception {
        mvc.perform(post("/client/emploi").session(client).with(csrf())
                        .param("employeur", "Employeur parcours")
                        .param("typeContrat", "CDI")
                        .param("dateDebut", LocalDate.now(clock).minusYears(5).toString())
                        .param("salaireNet", "50000")
                        .param("jourPaie", "25")
                        .param("salaireVerseSurCompte", "true")
                        .param("modeRemboursement", "RETENUE_EMPLOYEUR"))
                .andExpect(flash().attribute("succes", "client.emploi.enregistre"));
    }

    private void envoyerJustificatif(MockHttpSession client, TypeDocument type) throws Exception {
        mvc.perform(multipart("/client/justificatifs")
                        .file(new MockMultipartFile("fichier", "piece.pdf", "application/pdf", PDF))
                        .param("type", type.name()).session(client).with(csrf()))
                .andExpect(flash().attribute("succes", "client.justificatifs.envoye"));
    }

    private MockHttpSession connecterBackOffice(String identifiant, String motDePasse) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/bo/login").session(session).with(csrf())
                        .param("identifiant", identifiant).param("motDePasse", motDePasse))
                .andExpect(redirectedUrl("/bo/"));
        return session;
    }

    private void saisirPaiement(MockHttpSession recouvrement, String reference, String montant) throws Exception {
        mvc.perform(post("/bo/paiements").session(recouvrement).with(csrf())
                        .param("reference", reference).param("montant", montant)
                        .param("recuLe", LocalDate.now(clock).toString()))
                .andExpect(redirectedUrl("/bo/paiements"))
                .andExpect(flash().attributeExists("resultat"))
                .andExpect(flash().attributeCount(1));
    }

    // --- Lectures ------------------------------------------------------------------------------

    private Demande seuleDemande(UUID clientId) {
        List<Demande> liste = demandes.parClient(clientId);
        assertThat(liste).hasSize(1);
        return liste.getFirst();
    }

    /** Statut du dossier, lu par l'API publique du module demande (dossiers du client). */
    private StatutDossier statut(UUID demandeId) {
        return demandes.parClient(clientId).stream().filter(d -> d.id().equals(demandeId)).findFirst()
                .orElseThrow().statut();
    }

    private long ecrituresDe(UUID demandeId, NatureEcriture nature) {
        return comptabilite.duDossier(demandeId).stream().filter(e -> e.nature() == nature).count();
    }

    private static List<LigneEcriture> lignes(List<EcritureComptable> ecritures, NatureEcriture nature) {
        List<EcritureComptable> deNature = ecritures.stream().filter(e -> e.nature() == nature).toList();
        assertThat(deNature).hasSize(1);
        return deNature.getFirst().lignes();
    }

    private static Map<Compte, BigDecimal> soldes(List<EcritureComptable> ecritures) {
        Map<Compte, BigDecimal> soldes = new EnumMap<>(Compte.class);
        for (Compte c : Compte.values()) {
            soldes.put(c, BigDecimal.ZERO);
        }
        ecritures.stream().flatMap(e -> e.lignes().stream()).forEach(l -> soldes.merge(l.compte(),
                l.sens() == Sens.DEBIT ? l.montant().valeur() : l.montant().valeur().negate(), BigDecimal::add));
        return soldes;
    }

    /**
     * Les écouteurs {@code @ApplicationModuleListener} réagissent après la validation de la transaction,
     * éventuellement sur un autre fil : on attend leur effet, sans pause fixe.
     */
    private static void attendre(BooleanSupplier condition) throws InterruptedException {
        long limite = System.nanoTime() + DELAI_EVENEMENTS.toNanos();
        while (!condition.getAsBoolean()) {
            assertThat(System.nanoTime()).as("condition non atteinte dans le délai").isLessThan(limite);
            Thread.sleep(50);
        }
    }

    private static String telephoneUnique() {
        return "3" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    private static String nniUnique() {
        return "2" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
    }
}
