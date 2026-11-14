package com.seheli.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.seheli.catalogue.application.FournisseurService;
import com.seheli.catalogue.application.ProduitService;
import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.SuiviAutorisationService;
import com.seheli.demande.application.DemandeService;
import com.seheli.demande.domain.Demande;
import com.seheli.identite.application.ClientService;
import com.seheli.identite.application.DocumentService;
import com.seheli.identite.domain.Document;
import com.seheli.identite.domain.TypeDocument;

/**
 * Sécurité web par HTTP (MockMvc, filtres Spring Security réels, comptes de développement) :
 * accès par rôle, cloisonnement des clients, CSRF, envoi de fichiers et en-têtes de sécurité.
 */
class SecuriteIntegrationTest extends TestIntegration {

    private static final byte[] PDF = "%PDF-1.4\n% justificatif de test\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};
    private static final byte[] HTML = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);

    private static final String ANALYSTE = "ANALYSTE";
    private static final String RECOUVREMENT = "RECOUVREMENT";
    private static final String ADMIN = "ADMIN";
    private static final String ASSOCIE = "ASSOCIE";
    private static final List<String> ROLES = List.of(ANALYSTE, RECOUVREMENT, ADMIN, ASSOCIE);

    /** Comptes de développement (application-dev.yml), par rôle. */
    private static final Map<String, String[]> COMPTES = Map.of(
            ANALYSTE, new String[] {"analyste", "analyste-dev"},
            RECOUVREMENT, new String[] {"recouvrement", "recouvrement-dev"},
            ADMIN, new String[] {"admin", "admin-dev"},
            ASSOCIE, new String[] {"associe", "associe-dev"});

    /** Sessions back-office ouvertes une fois (BCrypt est lent) ; le contexte Spring est partagé. */
    private static final Map<String, MockHttpSession> SESSIONS = new ConcurrentHashMap<>();

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
    private SuiviAutorisationService autorisations;

    private MockMvc mvc;

    @BeforeEach
    void preparer() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // --- Accès par rôle ------------------------------------------------------------------------

    /** Pages GET du back-office et rôles autorisés : les autres rôles reçoivent 403. */
    private static final Map<String, Set<String>> PAGES_BACK_OFFICE = Map.ofEntries(
            Map.entry("/bo/", Set.copyOf(ROLES)),
            Map.entry("/bo/fournisseurs", Set.of(ADMIN)),
            Map.entry("/bo/fournisseurs/nouveau", Set.of(ADMIN)),
            Map.entry("/bo/produits", Set.of(ADMIN)),
            Map.entry("/bo/produits/nouveau", Set.of(ADMIN)),
            Map.entry("/bo/justificatifs", Set.of(ANALYSTE, ADMIN)),
            Map.entry("/bo/dossiers", Set.of(ANALYSTE)),
            Map.entry("/bo/autorisations", Set.of(ANALYSTE)),
            Map.entry("/bo/commandes", Set.of(ANALYSTE, ADMIN)),
            Map.entry("/bo/paiements", Set.of(RECOUVREMENT, ADMIN)),
            Map.entry("/bo/paiements/import", Set.of(RECOUVREMENT, ADMIN)),
            Map.entry("/bo/paiements/ecarts", Set.of(RECOUVREMENT, ADMIN)),
            Map.entry("/bo/recouvrement", Set.of(RECOUVREMENT, ADMIN)),
            Map.entry("/bo/capital", Set.of(ASSOCIE, ADMIN)),
            Map.entry("/bo/pilotage", Set.of(ASSOCIE, ADMIN)),
            Map.entry("/bo/inscriptions", Set.of(ASSOCIE, ADMIN)),
            Map.entry("/bo/inscriptions/export.csv", Set.of(ADMIN)),
            Map.entry("/actuator/metrics", Set.of(ADMIN)));

    @Test
    void chaquePageDuBackOfficeEstReserveeASesRoles() throws Exception {
        for (var page : PAGES_BACK_OFFICE.entrySet()) {
            for (String role : ROLES) {
                int attendu = page.getValue().contains(role) ? 200 : 403;
                MvcResult resultat = mvc.perform(get(page.getKey()).session(backOffice(role))).andReturn();
                assertThat(resultat.getResponse().getStatus())
                        .as("GET %s par %s", page.getKey(), role).isEqualTo(attendu);
            }
        }
    }

    /** Actions POST sensibles : refusées (403) aux rôles non autorisés, avant tout traitement. */
    @Test
    void lesActionsDuBackOfficeSontRefuseesAuxAutresRoles() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Set<String>> actions = Map.ofEntries(
                Map.entry("/bo/fournisseurs", Set.of(ADMIN)),
                Map.entry("/bo/produits", Set.of(ADMIN)),
                Map.entry("/bo/produits/" + id + "/disponibilite", Set.of(ADMIN)),
                Map.entry("/bo/dossiers/" + id + "/acceptation", Set.of(ANALYSTE)),
                Map.entry("/bo/dossiers/" + id + "/refus", Set.of(ANALYSTE)),
                Map.entry("/bo/autorisations/" + id + "/acceptation", Set.of(ANALYSTE)),
                Map.entry("/bo/commandes/" + id + "/validation", Set.of(ANALYSTE, ADMIN)),
                Map.entry("/bo/paiements", Set.of(RECOUVREMENT, ADMIN)),
                Map.entry("/bo/recouvrement/dossiers/" + id + "/solde", Set.of(RECOUVREMENT, ADMIN)),
                Map.entry("/bo/capital", Set.of(ADMIN)));
        for (var action : actions.entrySet()) {
            for (String role : ROLES) {
                if (action.getValue().contains(role)) {
                    continue;
                }
                mvc.perform(post(action.getKey()).session(backOffice(role)).with(csrf()))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void lAnonymeEstRenvoyeVersLaConnexionEtLesPagesPubliquesRestentOuvertes() throws Exception {
        for (String page : PAGES_BACK_OFFICE.keySet()) {
            if (page.startsWith("/bo")) {
                mvc.perform(get(page)).andExpect(redirectedUrl("/bo/login"));
            }
        }
        mvc.perform(get("/actuator/metrics")).andExpect(redirectedUrl("/bo/login"));
        for (String page : List.of("/client", "/client/catalogue", "/client/justificatifs", "/client/emploi",
                "/client/demandes", "/client/dossier", "/client/dossier/contrat.pdf")) {
            mvc.perform(get(page)).andExpect(redirectedUrl("/client/connexion"));
        }
        for (String page : List.of("/", "/inscription", "/bo/login", "/client/connexion", "/actuator/health")) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
        // La création de compte exige un téléphone prouvé par code : sinon retour à la connexion.
        mvc.perform(get("/client/connexion/compte")).andExpect(redirectedUrl("/client/connexion"));
    }

    @Test
    void unClientNEntrePasAuBackOfficeEtLEquipeNEntrePasDansLEspaceClient() throws Exception {
        MockHttpSession client = creerCompte().session();
        for (String page : PAGES_BACK_OFFICE.keySet()) {
            mvc.perform(get(page).session(client)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/bo/capital").session(client).with(csrf())).andExpect(status().isForbidden());
        for (String role : ROLES) {
            mvc.perform(get("/client/dossier").session(backOffice(role))).andExpect(status().isForbidden());
            mvc.perform(post("/client/demandes").session(backOffice(role)).with(csrf())
                    .param("produit", UUID.randomUUID().toString()).param("duree", "6"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void unMauvaisMotDePasseNOuvrePasLeBackOffice() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/bo/login").session(session).with(csrf())
                        .param("identifiant", "admin").param("motDePasse", "mauvais"))
                .andExpect(redirectedUrl("/bo/login?erreur"));
        mvc.perform(get("/bo/").session(session)).andExpect(redirectedUrl("/bo/login"));
    }

    @Test
    void laDeconnexionFermeLaSession() throws Exception {
        MockHttpSession client = creerCompte().session();
        mvc.perform(post("/client/logout").session(client).with(csrf()))
                .andExpect(redirectedUrl("/client/connexion?deconnexion"));
        mvc.perform(get("/client/dossier").session(client)).andExpect(redirectedUrl("/client/connexion"));
    }

    // --- Cloisonnement des clients -------------------------------------------------------------

    @Test
    void unClientNeVoitJamaisLesDonneesDUnAutre() throws Exception {
        ClientConnecte proprietaire = creerCompte();
        declarerEmploi(proprietaire.session(), "Employeur confidentiel");
        envoyerJustificatif(proprietaire.session(), TypeDocument.PIECE_IDENTITE, PDF);
        envoyerJustificatif(proprietaire.session(), TypeDocument.BULLETIN_PAIE, PDF);
        clients.verifierIdentite(proprietaire.id());
        UUID produitId = creerProduit();
        mvc.perform(post("/client/demandes").session(proprietaire.session()).with(csrf())
                        .param("produit", produitId.toString()).param("duree", "6"))
                .andExpect(redirectedUrl("/client/dossier"));
        Demande demande = demandes.parClient(proprietaire.id()).getFirst();
        mvc.perform(post("/bo/dossiers/{id}/acceptation", demande.id()).session(backOffice(ANALYSTE)).with(csrf()))
                .andExpect(flash().attribute("succes", "bo.decision.acceptee"));
        UUID documentId = documents.parClient(proprietaire.id()).getFirst().id();

        ClientConnecte curieux = creerCompte();
        MockHttpSession session = curieux.session();

        // Justificatif d'un autre : 404 comme un document inexistant, absent de la liste.
        mvc.perform(get("/client/justificatifs/{id}", documentId).session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/client/justificatifs/{id}", UUID.randomUUID()).session(session))
                .andExpect(status().isNotFound());
        assertThat(corps(get("/client/justificatifs").session(session))).doesNotContain(documentId.toString());
        // Ni son dossier, ni ses PDF, ni son emploi.
        assertThat(corps(get("/client/dossier").session(session)))
                .doesNotContain(demande.id().toString()).doesNotContain("Employeur confidentiel");
        assertThat(corps(get("/client/demandes").session(session))).doesNotContain(demande.id().toString());
        mvc.perform(get("/client/dossier/contrat.pdf").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/client/dossier/autorisation-prelevement.pdf").session(session))
                .andExpect(status().isNotFound());
        assertThat(corps(get("/client/emploi").session(session))).doesNotContain("Employeur confidentiel");
        // Un identifiant de client glissé dans la requête est ignoré : seul le principal compte.
        mvc.perform(post("/client/dossier/remise").session(session).with(csrf())
                        .param("confirmation", "on").param("clientId", proprietaire.id().toString()))
                .andExpect(redirectedUrl("/client/dossier"))
                .andExpect(flash().attributeExists("erreur"));
        mvc.perform(multipart("/client/justificatifs")
                        .file(new MockMultipartFile("fichier", "piece.pdf", "application/pdf", PDF))
                        .param("type", TypeDocument.RIB.name()).param("clientId", proprietaire.id().toString())
                        .session(session).with(csrf()))
                .andExpect(redirectedUrl("/client/justificatifs"));
        assertThat(documents.parClient(curieux.id())).extracting(Document::type).containsExactly(TypeDocument.RIB);
        assertThat(documents.parClient(proprietaire.id())).hasSize(2);

        // Le propriétaire, lui, accède à ses données ; le back-office aussi.
        mvc.perform(get("/client/justificatifs/{id}", documentId).session(proprietaire.session()))
                .andExpect(status().isOk());
        mvc.perform(get("/client/dossier/contrat.pdf").session(proprietaire.session()))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        mvc.perform(get("/bo/justificatifs/{id}", documentId).session(backOffice(ANALYSTE)))
                .andExpect(status().isOk());
        mvc.perform(get("/bo/justificatifs/{id}", documentId).session(backOffice(RECOUVREMENT)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/bo/justificatifs/{id}", documentId).session(backOffice(ASSOCIE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void laConnexionClientRenouvelleLIdentifiantDeSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String avant = session.getId();
        mvc.perform(post("/client/connexion").session(session).with(csrf()).param("telephone", telephoneUnique()))
                .andExpect(redirectedUrl("/client/connexion/code"));
        MvcResult resultat = mvc.perform(post("/client/connexion/code").session(session).with(csrf())
                        .param("code", CodeConnexionFixe.CODE))
                .andExpect(redirectedUrl("/client/connexion/compte"))
                .andReturn();
        assertThat(resultat.getRequest().getSession().getId()).isNotEqualTo(avant);
    }

    // --- CSRF ----------------------------------------------------------------------------------

    @Test
    void sansJetonCsrfToutPostEstRefuse() throws Exception {
        MockHttpSession client = creerCompte().session();
        List<AbstractMockHttpServletRequestBuilder<?>> requetes = List.of(
                post("/inscription").param("nom", "X").param("telephone", telephoneUnique()),
                post("/client/connexion").param("telephone", telephoneUnique()),
                post("/client/connexion/code").param("code", CodeConnexionFixe.CODE),
                post("/bo/login").param("identifiant", "admin").param("motDePasse", "admin-dev"),
                post("/client/emploi").session(client).param("employeur", "X"),
                post("/client/demandes").session(client).param("produit", UUID.randomUUID().toString())
                        .param("duree", "6"),
                post("/client/dossier/remise").session(client).param("confirmation", "on"),
                post("/client/logout").session(client),
                multipart("/client/justificatifs")
                        .file(new MockMultipartFile("fichier", "piece.pdf", "application/pdf", PDF))
                        .param("type", TypeDocument.RIB.name()).session(client),
                post("/bo/capital").session(backOffice(ADMIN)).param("montant", "1000"),
                post("/bo/paiements").session(backOffice(RECOUVREMENT)).param("reference", "X"),
                post("/bo/logout").session(backOffice(ADMIN)));
        for (AbstractMockHttpServletRequestBuilder<?> requete : requetes) {
            mvc.perform(requete).andExpect(status().isForbidden());
            mvc.perform(requete.with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        }
        // Rien n'a été enregistré ni fermé : la session client est toujours ouverte.
        mvc.perform(get("/client/justificatifs").session(client)).andExpect(status().isOk());
        mvc.perform(get("/bo/").session(backOffice(ADMIN))).andExpect(status().isOk());
    }

    @Test
    void lesFormulairesPortentLeJetonCsrf() throws Exception {
        MockHttpSession client = creerCompte().session();
        assertThat(corps(get("/client/justificatifs").session(client))).contains("name=\"_csrf\"");
        assertThat(corps(get("/client/emploi").session(client))).contains("name=\"_csrf\"");
        assertThat(corps(get("/inscription"))).contains("name=\"_csrf\"");
        assertThat(corps(get("/bo/login"))).contains("name=\"_csrf\"");
    }

    @Test
    void laDeconnexionNAcceptePasUnGet() throws Exception {
        mvc.perform(get("/bo/logout").session(backOffice(ADMIN)));
        mvc.perform(get("/bo/").session(backOffice(ADMIN))).andExpect(status().isOk());
    }

    // --- Envoi de fichiers ---------------------------------------------------------------------

    @Test
    void unJustificatifEstReconnuAuContenuEtNonAuNomDeFichier() throws Exception {
        ClientConnecte client = creerCompte();
        MockHttpSession session = client.session();

        // HTML ou exécutable déguisé en PDF : refusé, rien n'est stocké.
        for (byte[] contenu : List.of(HTML, "MZ\u0090\u0000".getBytes(StandardCharsets.ISO_8859_1))) {
            mvc.perform(multipart("/client/justificatifs")
                            .file(new MockMultipartFile("fichier", "piece.pdf", "application/pdf", contenu))
                            .param("type", TypeDocument.RIB.name()).session(session).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("erreur", "client.justificatifs.format.refuse"));
        }
        // Fichier vide ou absent.
        mvc.perform(multipart("/client/justificatifs")
                        .file(new MockMultipartFile("fichier", "vide.pdf", "application/pdf", new byte[0]))
                        .param("type", TypeDocument.RIB.name()).session(session).with(csrf()))
                .andExpect(model().attribute("erreur", "client.justificatifs.fichier.obligatoire"));
        mvc.perform(multipart("/client/justificatifs").param("type", TypeDocument.RIB.name())
                        .session(session).with(csrf()))
                .andExpect(model().attribute("erreur", "client.justificatifs.fichier.obligatoire"));
        // Au-delà de 5 Mo, même avec une signature PDF valide.
        mvc.perform(multipart("/client/justificatifs")
                        .file(new MockMultipartFile("fichier", "gros.pdf", "application/pdf", pdfDe(Document.TAILLE_MAX_OCTETS + 1)))
                        .param("type", TypeDocument.RIB.name()).session(session).with(csrf()))
                .andExpect(model().attribute("erreur", "client.justificatifs.fichier.trop-volumineux"));
        // Type de justificatif inconnu.
        mvc.perform(multipart("/client/justificatifs")
                        .file(new MockMultipartFile("fichier", "piece.pdf", "application/pdf", PDF))
                        .param("type", "INCONNU").session(session).with(csrf()))
                .andExpect(model().attribute("erreur", "client.justificatifs.type.obligatoire"));
        assertThat(documents.parClient(client.id())).isEmpty();

        // Exactement 5 Mo : accepté.
        envoyerJustificatif(session, TypeDocument.RIB, pdfDe(Document.TAILLE_MAX_OCTETS));
        // Un PNG annoncé comme PDF est servi comme PNG : le type vient du contenu, pas du navigateur.
        mvc.perform(multipart("/client/justificatifs")
                        .file(new MockMultipartFile("fichier", "../../etc/passwd.pdf", "application/pdf", PNG))
                        .param("type", TypeDocument.PIECE_IDENTITE.name()).session(session).with(csrf()))
                .andExpect(redirectedUrl("/client/justificatifs"));
        Document png = documents.parClient(client.id()).stream()
                .filter(d -> d.type() == TypeDocument.PIECE_IDENTITE).findFirst().orElseThrow();
        mvc.perform(get("/client/justificatifs/{id}", png.id()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.startsWith("inline"),
                                org.hamcrest.Matchers.containsString("piece-identite-"),
                                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwd")))))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void lAutorisationSigneeDeposeeAuBackOfficeEstControlee() throws Exception {
        UUID autorisationId = autorisationDUnDossierAccepte();
        String fiche = "/bo/autorisations/" + autorisationId;

        mvc.perform(multipart(fiche + "/document-signe")
                        .file(new MockMultipartFile("fichier", "autorisation.pdf", "application/pdf", HTML))
                        .session(backOffice(ANALYSTE)).with(csrf()))
                .andExpect(redirectedUrl(fiche))
                .andExpect(flash().attribute("erreur", "bo.autorisation.document.format-refuse"));
        mvc.perform(multipart(fiche + "/document-signe")
                        .file(new MockMultipartFile("fichier", "autorisation.pdf", "application/pdf",
                                pdfDe(Document.TAILLE_MAX_OCTETS + 1)))
                        .session(backOffice(ANALYSTE)).with(csrf()))
                .andExpect(flash().attribute("erreur", "bo.autorisation.document.trop-volumineux"));
        mvc.perform(multipart(fiche + "/document-signe").session(backOffice(ANALYSTE)).with(csrf()))
                .andExpect(flash().attribute("erreur", "bo.autorisation.document.vide"));
        // Réservé à l'analyste ; sans CSRF, refusé.
        for (String role : List.of(RECOUVREMENT, ADMIN, ASSOCIE)) {
            mvc.perform(multipart(fiche + "/document-signe")
                            .file(new MockMultipartFile("fichier", "autorisation.pdf", "application/pdf", PDF))
                            .session(backOffice(role)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(multipart(fiche + "/document-signe")
                        .file(new MockMultipartFile("fichier", "autorisation.pdf", "application/pdf", PDF))
                        .session(backOffice(ANALYSTE)))
                .andExpect(status().isForbidden());
        mvc.perform(get(fiche + "/document-signe").session(backOffice(ANALYSTE))).andExpect(status().isNotFound());

        mvc.perform(multipart(fiche + "/document-signe")
                        .file(new MockMultipartFile("fichier", "autorisation.pdf", "application/pdf", PDF))
                        .session(backOffice(ANALYSTE)).with(csrf()))
                .andExpect(flash().attribute("succes", "bo.autorisation.document.depose"));
        mvc.perform(get(fiche + "/document-signe").session(backOffice(ANALYSTE)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    // --- En-têtes HTTP -------------------------------------------------------------------------

    @Test
    void lesEnTetesDeSecuriteSontPresents() throws Exception {
        for (MockHttpServletRequestBuilder requete : List.of(get("/"), get("/client/connexion"), get("/bo/login"),
                get("/bo/").session(backOffice(ADMIN)))) {
            mvc.perform(requete)
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "same-origin"))
                    .andExpect(header().string("Content-Security-Policy",
                            org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("script-src 'self'"),
                                    org.hamcrest.Matchers.containsString("frame-ancestors 'none'"),
                                    org.hamcrest.Matchers.containsString("object-src 'none'"))))
                    .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        }
    }

    // --- Outils --------------------------------------------------------------------------------

    private MockHttpSession backOffice(String role) throws Exception {
        MockHttpSession session = SESSIONS.get(role);
        if (session != null && !session.isInvalid()) {
            return session;
        }
        String[] compte = COMPTES.get(role);
        session = new MockHttpSession();
        mvc.perform(post("/bo/login").session(session).with(csrf())
                        .param("identifiant", compte[0]).param("motDePasse", compte[1]))
                .andExpect(redirectedUrl("/bo/"));
        SESSIONS.put(role, session);
        return session;
    }

    private ClientConnecte creerCompte() throws Exception {
        String telephone = telephoneUnique();
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/client/connexion").session(session).with(csrf()).param("telephone", telephone))
                .andExpect(redirectedUrl("/client/connexion/code"));
        mvc.perform(post("/client/connexion/code").session(session).with(csrf())
                        .param("code", CodeConnexionFixe.CODE))
                .andExpect(redirectedUrl("/client/connexion/compte"));
        mvc.perform(post("/client/connexion/compte").session(session).with(csrf())
                        .param("nni", nniUnique()).param("nom", "Securite").param("prenom", "Client")
                        .param("dateNaissance", LocalDate.now(clock).minusYears(30).toString())
                        .param("consentement", "true"))
                .andExpect(redirectedUrl("/client/"));
        return new ClientConnecte(session, clients.parTelephone(telephone).orElseThrow().id());
    }

    private void declarerEmploi(MockHttpSession client, String employeur) throws Exception {
        mvc.perform(post("/client/emploi").session(client).with(csrf())
                        .param("employeur", employeur)
                        .param("typeContrat", "CDI")
                        .param("dateDebut", LocalDate.now(clock).minusYears(5).toString())
                        .param("salaireNet", "50000")
                        .param("jourPaie", "25")
                        .param("salaireVerseSurCompte", "true")
                        .param("modeRemboursement", "RETENUE_EMPLOYEUR"))
                .andExpect(flash().attribute("succes", "client.emploi.enregistre"));
    }

    private void envoyerJustificatif(MockHttpSession client, TypeDocument type, byte[] contenu) throws Exception {
        mvc.perform(multipart("/client/justificatifs")
                        .file(new MockMultipartFile("fichier", "piece.pdf", "application/pdf", contenu))
                        .param("type", type.name()).session(client).with(csrf()))
                .andExpect(flash().attribute("succes", "client.justificatifs.envoye"));
    }

    private UUID creerProduit() {
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        UUID fournisseurId = fournisseurs.creer("Fournisseur sécurité " + suffixe, new BigDecimal("8")).id();
        return produits.creer("SEC-" + suffixe, "Téléphone sécurité", "Marque", CategorieProduit.TELEPHONE,
                fournisseurId, Montant.de(20_000)).id();
    }

    private UUID autorisationDUnDossierAccepte() throws Exception {
        ClientConnecte client = creerCompte();
        declarerEmploi(client.session(), "Employeur sécurité");
        envoyerJustificatif(client.session(), TypeDocument.PIECE_IDENTITE, PDF);
        envoyerJustificatif(client.session(), TypeDocument.BULLETIN_PAIE, PDF);
        clients.verifierIdentite(client.id());
        mvc.perform(post("/client/demandes").session(client.session()).with(csrf())
                        .param("produit", creerProduit().toString()).param("duree", "6"))
                .andExpect(redirectedUrl("/client/dossier"));
        UUID demandeId = demandes.parClient(client.id()).getFirst().id();
        mvc.perform(post("/bo/dossiers/{id}/acceptation", demandeId).session(backOffice(ANALYSTE)).with(csrf()))
                .andExpect(flash().attribute("succes", "bo.decision.acceptee"));
        return autorisations.parDemande(demandeId).orElseThrow().id();
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static byte[] pdfDe(long taille) {
        byte[] contenu = new byte[Math.toIntExact(taille)];
        Arrays.fill(contenu, (byte) ' ');
        System.arraycopy(PDF, 0, contenu, 0, PDF.length);
        return contenu;
    }

    private record ClientConnecte(MockHttpSession session, UUID id) {
    }

    private static String telephoneUnique() {
        return "3" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    private static String nniUnique() {
        return "2" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
    }
}
