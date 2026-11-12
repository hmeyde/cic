package com.cic.contrat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.audit.domain.EntreeJournal;
import com.cic.audit.domain.TypeActeur;
import com.cic.commun.domain.Montant;
import com.cic.contrat.AutorisationPrelevementStatutChange;
import com.cic.contrat.domain.AutorisationPrelevement;
import com.cic.contrat.domain.DepotDocumentSigneImpossibleException;
import com.cic.contrat.domain.Prix;
import com.cic.contrat.domain.StatutAutorisation;
import com.cic.contrat.domain.TransitionAutorisationInterditeException;
import com.cic.identite.application.ClientService;
import com.cic.identite.application.ClientsEnMemoire;
import com.cic.identite.domain.Client;
import com.cic.identite.domain.FormatDocument;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class SuiviAutorisationServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant MAINTENANT = Instant.parse("2026-11-14T10:00:00Z");
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};

    private final ContratsEnMemoire contrats = new ContratsEnMemoire();
    private final AutorisationsEnMemoire autorisations = new AutorisationsEnMemoire();
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final StockageSignesEnMemoire stockage = new StockageSignesEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(new JournalEnMemoire());
    private final List<Object> evenements = new ArrayList<>();
    private final Clock clock = Clock.fixed(MAINTENANT, NOUAKCHOTT);
    private final ContratService contratService;
    private final SuiviAutorisationService service;
    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Mohamed", LocalDate.of(1990, 5, 1),
            "22123456", MAINTENANT);

    SuiviAutorisationServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        contratService = new ContratService(contrats, autorisations, parametres, clock);
        service = new SuiviAutorisationService(autorisations, contrats, new ClientService(clients), stockage, journal,
                evenements::add, clock);
        clients.enregistrer(client);
    }

    private AutorisationPrelevement emise() {
        UUID demandeId = UUID.randomUUID();
        contratService.etablir(new ContratAEtablir(demandeId, client.id(), UUID.randomUUID(), UUID.randomUUID(),
                "Galaxy A35", new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), 6, 25, Optional.empty(), MAINTENANT.minusSeconds(86_400)));
        return service.parDemande(demandeId).orElseThrow();
    }

    private List<EntreeJournal> journalDe(AutorisationPrelevement a) {
        return journal.historique(SuiviAutorisationService.objet(a.id()));
    }

    @Test
    void l_analyste_fait_passer_une_autorisation_a_acceptee() {
        AutorisationPrelevement a = emise();

        service.deposerDocumentSigne(a.id(), "analyste", PDF);
        service.transmettre(a.id(), "analyste");
        service.accepter(a.id(), "analyste");

        AutorisationPrelevement relue = autorisations.parId(a.id()).orElseThrow();
        assertThat(relue.statut()).isEqualTo(StatutAutorisation.ACCEPTEE);
        assertThat(relue.statutChangeLe()).isEqualTo(MAINTENANT);
        assertThat(autorisations.verrous()).isEqualTo(3);
        assertThat(journalDe(a)).extracting(EntreeJournal::action)
                .containsExactly("DEPOT_DOCUMENT_SIGNE", "TRANSMISSION", "ACCEPTATION");
        assertThat(journalDe(a).getLast()).satisfies(e -> {
            assertThat(e.avant()).contains("TRANSMISE");
            assertThat(e.apres()).contains("ACCEPTEE");
            assertThat(e.acteur().type()).isEqualTo(TypeActeur.BACK_OFFICE);
            assertThat(e.acteur().identifiant()).isEqualTo("analyste");
        });
        assertThat(evenements.getLast()).isEqualTo(new AutorisationPrelevementStatutChange(a.id(), a.demandeId(),
                a.clientId(), StatutAutorisation.TRANSMISE, StatutAutorisation.ACCEPTEE, MAINTENANT));
    }

    @Test
    void le_document_signe_depose_est_stocke_et_relu_par_le_back_office() {
        AutorisationPrelevement a = emise();

        service.deposerDocumentSigne(a.id(), "analyste", PDF);

        assertThat(service.documentSigne(a.id())).hasValueSatisfying(c -> {
            assertThat(c.contenu()).isEqualTo(PDF);
            assertThat(c.document().format()).isEqualTo(FormatDocument.PDF);
            assertThat(c.document().tailleOctets()).isEqualTo(PDF.length);
            assertThat(c.document().deposePar()).isEqualTo("analyste");
            assertThat(c.document().deposeLe()).isEqualTo(MAINTENANT);
        });
        assertThat(autorisations.parId(a.id()).orElseThrow().statut()).isEqualTo(StatutAutorisation.SIGNEE);
        assertThat(evenements).containsExactly(new AutorisationPrelevementStatutChange(a.id(), a.demandeId(),
                a.clientId(), StatutAutorisation.A_SIGNER, StatutAutorisation.SIGNEE, MAINTENANT));
    }

    @Test
    void remplacer_le_document_garde_le_statut_et_l_ancien_fichier() {
        AutorisationPrelevement a = emise();
        service.deposerDocumentSigne(a.id(), "analyste", PDF);

        service.deposerDocumentSigne(a.id(), "analyste", PNG);

        assertThat(service.documentSigne(a.id())).hasValueSatisfying(
                c -> assertThat(c.document().format()).isEqualTo(FormatDocument.PNG));
        assertThat(stockage.nombre()).isEqualTo(2);
        assertThat(journalDe(a).getLast()).satisfies(e -> {
            assertThat(e.action()).isEqualTo("REMPLACEMENT_DOCUMENT_SIGNE");
            assertThat(e.apres()).isEmpty();
        });
        assertThat(evenements).hasSize(1);
    }

    @Test
    void refuse_un_document_vide_trop_gros_ou_d_un_autre_format() {
        AutorisationPrelevement a = emise();
        byte[] tropGros = new byte[(int) com.cic.contrat.domain.DocumentSigne.TAILLE_MAX_OCTETS + 1];
        System.arraycopy(PDF, 0, tropGros, 0, PDF.length);

        assertThatThrownBy(() -> service.deposerDocumentSigne(a.id(), "analyste", new byte[0]))
                .isInstanceOf(SaisieAutorisationInvalideException.class)
                .hasFieldOrPropertyWithValue("code", SaisieAutorisationInvalideException.DOCUMENT_VIDE);
        assertThatThrownBy(() -> service.deposerDocumentSigne(a.id(), "analyste", tropGros))
                .hasFieldOrPropertyWithValue("code", SaisieAutorisationInvalideException.DOCUMENT_TROP_VOLUMINEUX);
        assertThatThrownBy(() -> service.deposerDocumentSigne(a.id(), "analyste", "texte".getBytes()))
                .hasFieldOrPropertyWithValue("code", SaisieAutorisationInvalideException.FORMAT_REFUSE);
        assertThat(stockage.nombre()).isZero();
        assertThat(autorisations.parId(a.id()).orElseThrow().statut()).isEqualTo(StatutAutorisation.A_SIGNER);
    }

    @Test
    void une_transition_interdite_ne_laisse_aucune_trace() {
        AutorisationPrelevement a = emise();

        assertThatThrownBy(() -> service.accepter(a.id(), "analyste"))
                .isInstanceOf(TransitionAutorisationInterditeException.class);

        assertThat(autorisations.misesAJour()).isZero();
        assertThat(journalDe(a)).isEmpty();
        assertThat(evenements).isEmpty();
    }

    @Test
    void aucun_depot_apres_la_transmission() {
        AutorisationPrelevement a = emise();
        service.deposerDocumentSigne(a.id(), "analyste", PDF);
        service.transmettre(a.id(), "analyste");

        assertThatThrownBy(() -> service.deposerDocumentSigne(a.id(), "analyste", PNG))
                .isInstanceOf(DepotDocumentSigneImpossibleException.class);
        assertThat(stockage.nombre()).isEqualTo(1);
    }

    @Test
    void la_revocation_exige_un_motif_conserve_au_journal() {
        AutorisationPrelevement a = emise();
        service.deposerDocumentSigne(a.id(), "analyste", PDF);

        assertThatThrownBy(() -> service.revoquer(a.id(), "analyste", "  "))
                .hasFieldOrPropertyWithValue("code", SaisieAutorisationInvalideException.MOTIF_INVALIDE);
        assertThatThrownBy(() -> service.revoquer(a.id(), "analyste",
                "x".repeat(SuiviAutorisationService.LONGUEUR_MOTIF_MAX + 1)))
                .hasFieldOrPropertyWithValue("code", SaisieAutorisationInvalideException.MOTIF_INVALIDE);

        service.revoquer(a.id(), "analyste", " Compte clôturé par le client ");

        assertThat(autorisations.parId(a.id()).orElseThrow().statut()).isEqualTo(StatutAutorisation.REVOQUEE);
        assertThat(journalDe(a).getLast().detail()).contains("Compte clôturé par le client");
    }

    @Test
    void une_autorisation_inconnue_est_introuvable() {
        assertThatThrownBy(() -> service.transmettre(UUID.randomUUID(), "analyste"))
                .isInstanceOf(AutorisationIntrouvableException.class);
        assertThat(service.fiche(UUID.randomUUID())).isEmpty();
        assertThat(service.documentSigne(UUID.randomUUID())).isEmpty();
    }

    @Test
    void la_liste_filtre_par_statut_avec_contrat_et_client() {
        AutorisationPrelevement signee = emise();
        emise();
        service.deposerDocumentSigne(signee.id(), "analyste", PDF);

        assertThat(service.liste(Optional.empty(), 100)).hasSize(2);
        assertThat(service.liste(Optional.of(StatutAutorisation.SIGNEE), 100)).singleElement().satisfies(l -> {
            assertThat(l.autorisation().id()).isEqualTo(signee.id());
            assertThat(l.numeroContrat()).startsWith("CIC-2026-");
            assertThat(l.designation()).isEqualTo("Galaxy A35");
            assertThat(l.client()).isEqualTo("Mohamed Ould Ahmed");
        });
    }
}
