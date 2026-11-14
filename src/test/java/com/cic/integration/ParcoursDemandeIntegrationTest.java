package com.cic.integration;

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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.domain.EntreeJournal;
import com.cic.audit.domain.ObjetAudite;
import com.cic.catalogue.application.FournisseurService;
import com.cic.catalogue.application.ProduitService;
import com.cic.catalogue.domain.CategorieProduit;
import com.cic.commun.domain.Montant;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.domain.Contrat;
import com.cic.demande.application.DemandeService;
import com.cic.demande.domain.ConditionEliminatoire;
import com.cic.demande.domain.CritereScore;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.Recommandation;
import com.cic.demande.domain.StatutDossier;
import com.cic.identite.application.ClientService;
import com.cic.identite.application.DocumentService;
import com.cic.identite.domain.Document;
import com.cic.identite.domain.TypeDocument;

/**
 * Parcours de demande de bout en bout, par HTTP (MockMvc, sécurité et CSRF actifs) sur PostgreSQL :
 * création du compte par code à usage unique, emploi, justificatifs, demande, score, puis décision de
 * l'analyste connecté au back-office par identifiant et mot de passe.
 *
 * <p>Seule étape hors HTTP : la vérification de l'identité par le back-office, qui n'a pas encore
 * d'écran ; elle passe par l'API publique du module identite ({@link ClientService#verifierIdentite}).
 * Chaque test crée son client, son fournisseur et son produit : la base est partagée.
 */
class ParcoursDemandeIntegrationTest extends TestIntegration {

    private static final String ANALYSTE = "analyste";
    private static final String ANALYSTE_MOT_DE_PASSE = "analyste-dev";
    private static final byte[] PDF = "%PDF-1.4\n% justificatif de test\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private WebApplicationContext contexte;
    @Autowired
    private Clock clock;
    @Autowired
    private FournisseurService fournisseurs;
    @Autowired
    private ProduitService produits;
    @Autowired
    private ClientService clients;
    @Autowired
    private DocumentService documents;
    @Autowired
    private DemandeService demandes;
    @Autowired
    private ContratService contrats;
    @Autowired
    private JournalAuditService journal;

    private MockMvc mvc;
    private UUID produitId;

    @BeforeEach
    void preparer() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        // Exemple chiffré de CLAUDE.md : prix public 20 000 MRU, remise fournisseur 8 %.
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        UUID fournisseurId = fournisseurs.creer("Fournisseur intégration " + suffixe, new BigDecimal("8")).id();
        produitId = produits.creer("IT-" + suffixe, "Téléphone intégration", "Marque", CategorieProduit.TELEPHONE,
                fournisseurId, Montant.de(20_000)).id();
    }

    @Test
    void demandeAccepteeDeBoutEnBout() throws Exception {
        ClientConnecte connecte = creerCompte();
        MockHttpSession client = connecte.session();
        UUID clientId = connecte.id();
        declarerEmploiCdi(client);

        envoyerJustificatif(client, TypeDocument.PIECE_IDENTITE);
        envoyerJustificatif(client, TypeDocument.BULLETIN_PAIE);
        assertThat(documents.parClient(clientId)).extracting(Document::type)
                .containsExactlyInAnyOrder(TypeDocument.PIECE_IDENTITE, TypeDocument.BULLETIN_PAIE);
        mvc.perform(get("/client/justificatifs").session(client)).andExpect(status().isOk());

        clients.verifierIdentite(clientId);

        mvc.perform(post("/client/demandes").session(client).with(csrf())
                        .param("produit", produitId.toString()).param("duree", "6"))
                .andExpect(redirectedUrl("/client/dossier"))
                .andExpect(flash().attribute("succes", "client.demande.soumise"));

        Demande demande = seuleDemande(clientId);
        assertThat(demande.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
        assertThat(demande.termes().prixVente()).isEqualTo(Montant.de(20_800));
        assertThat(demande.termes().apport()).isEqualTo(Montant.de(8_320));
        assertThat(demande.termes().mensualite()).isEqualTo(Montant.de(2_080));

        // Score v1 : CDI 25 + ancienneté > 2 ans 15 + retenue employeur 15 + part du salaire < 20 % 15 = 70.
        var score = demande.score();
        assertThat(score.eligibilite().estEligible()).isTrue();
        assertThat(score.note().resultat(CritereScore.TYPE_CONTRAT).points()).isEqualTo(25);
        assertThat(score.note().resultat(CritereScore.ANCIENNETE).points()).isEqualTo(15);
        assertThat(score.note().resultat(CritereScore.MODE_REMBOURSEMENT).points()).isEqualTo(15);
        assertThat(score.note().resultat(CritereScore.PART_SALAIRE).points()).isEqualTo(15);
        assertThat(score.note().resultat(CritereScore.APPORT_SUPPLEMENTAIRE).points()).isZero();
        assertThat(score.note().resultat(CritereScore.GARANT).points()).isZero();
        assertThat(score.note().resultat(CritereScore.HISTORIQUE_CIC).points()).isZero();
        assertThat(score.note().note()).isEqualTo(70);
        assertThat(score.recommandation()).isEqualTo(Recommandation.FAVORABLE);
        assertThat(score.versionGrille()).isNotBlank();

        mvc.perform(get("/client/dossier").session(client)).andExpect(status().isOk());

        MockHttpSession analyste = connecterAnalyste();
        mvc.perform(get("/bo/dossiers").session(analyste)).andExpect(status().isOk());
        mvc.perform(get("/bo/dossiers/{id}", demande.id()).session(analyste)).andExpect(status().isOk());

        mvc.perform(post("/bo/dossiers/{id}/acceptation", demande.id()).session(analyste).with(csrf()))
                .andExpect(redirectedUrl("/bo/dossiers/" + demande.id()))
                .andExpect(flash().attribute("succes", "bo.decision.acceptee"));

        Demande acceptee = seuleDemande(clientId);
        assertThat(acceptee.statut()).isEqualTo(StatutDossier.ACCEPTEE);
        assertThat(acceptee.decision()).hasValueSatisfying(d -> assertThat(d.analyste()).isEqualTo(ANALYSTE));
        Contrat contrat = contrats.parDemande(demande.id()).orElseThrow();
        assertThat(contrat.apport()).isEqualTo(Montant.de(8_320));
        assertThat(actionsJournalisees(demande.id()))
                .containsExactly("SOUMISSION", "MISE_EN_ANALYSE", "ACCEPTATION");

        mvc.perform(get("/client/dossier").session(client)).andExpect(status().isOk());
        mvc.perform(get("/client/dossier/contrat.pdf").session(client))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void identiteNonVerifieeJustificatifDemandePuisRefus() throws Exception {
        ClientConnecte connecte = creerCompte();
        MockHttpSession client = connecte.session();
        UUID clientId = connecte.id();
        declarerEmploiCdi(client);
        envoyerJustificatif(client, TypeDocument.BULLETIN_PAIE);

        mvc.perform(post("/client/demandes").session(client).with(csrf())
                        .param("produit", produitId.toString()).param("duree", "6"))
                .andExpect(redirectedUrl("/client/dossier"));

        // Le score recommande sans bloquer : identité non vérifiée, la demande reste à l'analyste.
        Demande demande = seuleDemande(clientId);
        assertThat(demande.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
        assertThat(demande.score().eligibilite().estEligible()).isFalse();
        assertThat(demande.score().eligibilite().nonSatisfaites()).extracting(r -> r.condition())
                .containsExactly(ConditionEliminatoire.IDENTITE_VERIFIEE);

        MockHttpSession analyste = connecterAnalyste();
        String fiche = "/bo/dossiers/" + demande.id();

        mvc.perform(post(fiche + "/justificatif").session(analyste).with(csrf())
                        .param("type", TypeDocument.PIECE_IDENTITE.name()).param("precision", "recto et verso"))
                .andExpect(redirectedUrl(fiche))
                .andExpect(flash().attribute("succes", "bo.decision.justificatif-demande"));
        assertThat(seuleDemande(clientId).statut()).isEqualTo(StatutDossier.EN_ANALYSE);

        mvc.perform(post(fiche + "/refus").session(analyste).with(csrf()).param("motif", " "))
                .andExpect(redirectedUrl(fiche))
                .andExpect(flash().attributeExists("erreur"));
        assertThat(seuleDemande(clientId).statut()).isEqualTo(StatutDossier.EN_ANALYSE);

        mvc.perform(post(fiche + "/refus").session(analyste).with(csrf())
                        .param("motif", "Pièce d'identité non fournie"))
                .andExpect(redirectedUrl(fiche))
                .andExpect(flash().attribute("succes", "bo.decision.refusee"));

        Demande refusee = seuleDemande(clientId);
        assertThat(refusee.statut()).isEqualTo(StatutDossier.REFUSEE);
        assertThat(refusee.decision().flatMap(d -> d.motif())).contains("Pièce d'identité non fournie");
        assertThat(contrats.parDemande(demande.id())).isEmpty();
        assertThat(actionsJournalisees(demande.id()))
                .containsExactly("SOUMISSION", "MISE_EN_ANALYSE", "DEMANDE_JUSTIFICATIF", "REFUS");

        // Une décision déjà prise ne se rejoue pas : REFUSEE → ACCEPTEE est interdite.
        mvc.perform(post(fiche + "/acceptation").session(analyste).with(csrf()))
                .andExpect(redirectedUrl(fiche))
                .andExpect(flash().attributeExists("erreur"));
        assertThat(seuleDemande(clientId).statut()).isEqualTo(StatutDossier.REFUSEE);
    }

    @Test
    void unSeulDossierActifEtEspacesCloisonnes() throws Exception {
        ClientConnecte connecte = creerCompte();
        MockHttpSession client = connecte.session();
        UUID clientId = connecte.id();
        declarerEmploiCdi(client);

        mvc.perform(post("/client/demandes").session(client).with(csrf())
                        .param("produit", produitId.toString()).param("duree", "6"))
                .andExpect(redirectedUrl("/client/dossier"));
        mvc.perform(post("/client/demandes").session(client).with(csrf())
                        .param("produit", produitId.toString()).param("duree", "3"))
                .andExpect(redirectedUrl("/client/catalogue/" + produitId + "?duree=3"))
                .andExpect(flash().attributeExists("erreur"));
        assertThat(demandes.parClient(clientId)).hasSize(1);

        // Sans jeton CSRF, la soumission est rejetée.
        mvc.perform(post("/client/demandes").session(client)
                        .param("produit", produitId.toString()).param("duree", "6"))
                .andExpect(status().isForbidden());

        // Le client connecté n'a pas le rôle ANALYSTE : 403. L'anonyme est renvoyé vers la connexion.
        mvc.perform(get("/bo/dossiers").session(client)).andExpect(status().isForbidden());
        mvc.perform(get("/bo/dossiers")).andExpect(redirectedUrl("/bo/login"));
        mvc.perform(get("/client/dossier")).andExpect(redirectedUrl("/client/connexion"));

        // Un justificatif d'un autre client répond 404.
        ClientConnecte autreClient = creerCompte();
        MockHttpSession autre = autreClient.session();
        envoyerJustificatif(autre, TypeDocument.RIB);
        UUID documentAutre = documents.parClient(autreClient.id()).getFirst().id();
        mvc.perform(get("/client/justificatifs/{id}", documentAutre).session(client))
                .andExpect(status().isNotFound());
        mvc.perform(get("/client/justificatifs/{id}", documentAutre).session(autre))
                .andExpect(status().isOk());
    }

    // --- Étapes du parcours -------------------------------------------------------------------

    /** Connexion par code à usage unique d'un téléphone inconnu, puis création du compte. */
    private ClientConnecte creerCompte() throws Exception {
        String telephone = telephoneUnique();
        String nni = nniUnique();
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/client/connexion").session(session).with(csrf()).param("telephone", telephone))
                .andExpect(redirectedUrl("/client/connexion/code"));
        mvc.perform(post("/client/connexion/code").session(session).with(csrf())
                        .param("code", CodeConnexionFixe.CODE))
                .andExpect(redirectedUrl("/client/connexion/compte"));
        mvc.perform(post("/client/connexion/compte").session(session).with(csrf())
                        .param("nni", nni).param("nom", "Test").param("prenom", "Intégration")
                        .param("dateNaissance", LocalDate.now(clock).minusYears(30).toString())
                        .param("consentement", "true"))
                .andExpect(redirectedUrl("/client/"))
                .andExpect(flash().attribute("succes", "client.compte.cree"));
        mvc.perform(get("/client/").session(session)).andExpect(status().isOk());
        return new ClientConnecte(session, clients.parTelephone(telephone).orElseThrow().id());
    }

    /** CDI depuis 5 ans, 50 000 MRU nets versés sur un compte, remboursement par retenue de l'employeur. */
    private void declarerEmploiCdi(MockHttpSession client) throws Exception {
        mvc.perform(post("/client/emploi").session(client).with(csrf())
                        .param("employeur", "Employeur intégration")
                        .param("typeContrat", "CDI")
                        .param("dateDebut", LocalDate.now(clock).minusYears(5).toString())
                        .param("salaireNet", "50000")
                        .param("jourPaie", "25")
                        .param("salaireVerseSurCompte", "true")
                        .param("modeRemboursement", "RETENUE_EMPLOYEUR"))
                .andExpect(redirectedUrl("/client/emploi"))
                .andExpect(flash().attribute("succes", "client.emploi.enregistre"));
    }

    private void envoyerJustificatif(MockHttpSession client, TypeDocument type) throws Exception {
        mvc.perform(multipart("/client/justificatifs")
                        .file(new MockMultipartFile("fichier", "piece.pdf", "application/pdf", PDF))
                        .param("type", type.name()).session(client).with(csrf()))
                .andExpect(redirectedUrl("/client/justificatifs"))
                .andExpect(flash().attribute("succes", "client.justificatifs.envoye"));
    }

    private MockHttpSession connecterAnalyste() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/bo/login").session(session).with(csrf())
                        .param("identifiant", ANALYSTE).param("motDePasse", ANALYSTE_MOT_DE_PASSE))
                .andExpect(redirectedUrl("/bo/"));
        return session;
    }

    private Demande seuleDemande(UUID clientId) {
        List<Demande> liste = demandes.parClient(clientId);
        assertThat(liste).hasSize(1);
        return liste.getFirst();
    }

    private List<String> actionsJournalisees(UUID demandeId) {
        return journal.historique(new ObjetAudite("DEMANDE", demandeId)).stream()
                .map(EntreeJournal::action).toList();
    }

    // --- Données uniques (base partagée) ------------------------------------------------------

    /** Session HTTP du client connecté et son identifiant. */
    private record ClientConnecte(MockHttpSession session, UUID id) {
    }

    /** Téléphone mauritanien à 8 chiffres, jamais celui du client de démonstration (22000001). */
    private static String telephoneUnique() {
        return "4" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    private static String nniUnique() {
        return "1" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
    }
}
