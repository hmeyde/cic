package com.cic.demande.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.audit.domain.TypeActeur;
import com.cic.commun.domain.Montant;
import com.cic.demande.application.DecisionService;
import com.cic.demande.application.DecisionsDeTest;
import com.cic.demande.application.DemandesEnMemoire;
import com.cic.demande.application.FicheDossierService;
import com.cic.demande.domain.ConditionEliminatoire;
import com.cic.demande.domain.CritereScore;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandesFixtures;
import com.cic.demande.domain.ResultatCondition;
import com.cic.demande.domain.ResultatCritere;
import com.cic.demande.domain.StatutDossier;
import com.cic.identite.application.ClientService;
import com.cic.identite.application.ClientsEnMemoire;
import com.cic.identite.application.DocumentService;
import com.cic.identite.application.DocumentsEnMemoire;
import com.cic.identite.application.EmploiClientService;
import com.cic.identite.application.EmploisEnMemoire;
import com.cic.identite.application.StockageEnMemoire;
import com.cic.identite.domain.Client;
import com.cic.identite.domain.Document;
import com.cic.identite.domain.Emploi;
import com.cic.identite.domain.FormatDocument;
import com.cic.identite.domain.ModeRemboursement;
import com.cic.identite.domain.TypeContrat;
import com.cic.identite.domain.TypeDocument;

class FicheDossierBackOfficeControllerTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");

    private final Clock clock = Clock.fixed(LocalDate.of(2026, 11, 12).atTime(10, 0).atZone(NOUAKCHOTT).toInstant(),
            NOUAKCHOTT);
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final EmploisEnMemoire emplois = new EmploisEnMemoire();
    private final DocumentsEnMemoire documents = new DocumentsEnMemoire();
    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final JournalEnMemoire journal = new JournalEnMemoire();
    private final FicheDossierBackOfficeController controller = new FicheDossierBackOfficeController(
            new FicheDossierService(demandes, new ClientService(clients),
                    new EmploiClientService(clients, emplois, clock),
                    new DocumentService(clients, documents, new StockageEnMemoire(), clock),
                    new JournalAuditService(journal)),
            clock);

    private VueFicheDossier ouvrir(UUID id) {
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.fiche(id, model)).isEqualTo("bo/dossiers/fiche");
        return (VueFicheDossier) model.get("fiche");
    }

    private Demande demande(UUID clientId) {
        Demande d = DemandesFixtures.demande(clientId, StatutDossier.EN_ANALYSE);
        demandes.enregistrer(d);
        return d;
    }

    @Test
    void l_analyste_voit_client_emploi_justificatifs_et_score_detaille() {
        Client client = Client.nouveau("1000000001", "Ould Ahmed", "Mohamed", LocalDate.of(1990, 5, 1), "22120001",
                clock.instant());
        client.verifierNni();
        clients.enregistrer(client);
        emplois.enregistrer(Emploi.nouveau(client.id(), "SOMELEC", TypeContrat.CDI, LocalDate.of(2020, 1, 1), null,
                Montant.de(30_000), 25, true, true, ModeRemboursement.RETENUE_EMPLOYEUR));
        documents.enregistrer(Document.nouveau(client.id(), TypeDocument.PIECE_IDENTITE, FormatDocument.PDF, 2_000,
                Instant.parse("2026-11-11T09:00:00Z")));
        Demande demande = demande(client.id());

        VueFicheDossier fiche = ouvrir(demande.id());

        assertThat(fiche.id()).isEqualTo(demande.id());
        assertThat(fiche.statut()).isEqualTo(StatutDossier.EN_ANALYSE);
        assertThat(fiche.conditions().prixVente()).isEqualTo(Montant.de(20_800));
        assertThat(fiche.conditions().resteAFinancer()).isEqualTo(Montant.de(12_480));
        assertThat(fiche.conditions().derniereDiffere()).isFalse();

        assertThat(fiche.client()).hasValueSatisfying(c -> {
            assertThat(c.nomComplet()).isEqualTo("Mohamed Ould Ahmed");
            assertThat(c.age()).isEqualTo(36);
            assertThat(c.nniVerifie()).isTrue();
            assertThat(c.pieceIdentiteVerifiee()).isFalse();
        });
        assertThat(fiche.emploi()).hasValueSatisfying(e -> {
            assertThat(e.employeur()).isEqualTo("SOMELEC");
            assertThat(e.ancienneteAnnees()).isEqualTo(6);
            assertThat(e.ancienneteMois()).isEqualTo(10);
            assertThat(e.dateFin()).isEmpty();
            assertThat(e.partSalairePourcent()).isEqualTo(7); // 2 080 / 30 000
        });
        assertThat(fiche.justificatifs()).singleElement().satisfies(j -> {
            assertThat(j.type()).isEqualTo(TypeDocument.PIECE_IDENTITE);
            assertThat(j.tailleKo()).isEqualTo(2);
            assertThat(j.recuLe()).isEqualTo("11/11/2026 09:00");
        });
        assertThat(fiche.typesManquants()).containsExactly(TypeDocument.CONTRAT_OU_ATTESTATION,
                TypeDocument.BULLETIN_PAIE, TypeDocument.RIB);

        var attendu = DemandesFixtures.score();
        assertThat(fiche.score().note()).isEqualTo(attendu.note().note());
        assertThat(fiche.score().versionGrille()).isEqualTo("v1");
        assertThat(fiche.score().recommandation()).isEqualTo(attendu.recommandation().name());
        assertThat(fiche.score().criteres()).extracting(ResultatCritere::critere)
                .containsExactly(CritereScore.values());
        assertThat(fiche.score().criteres()).allSatisfy(r -> assertThat(r.raison()).isNotBlank());
        assertThat(fiche.score().conditions()).extracting(ResultatCondition::condition)
                .containsExactly(ConditionEliminatoire.values());
    }

    @Test
    void client_et_emploi_introuvables_restent_affichables() {
        Demande demande = demande(UUID.randomUUID());

        VueFicheDossier fiche = ouvrir(demande.id());

        assertThat(fiche.client()).isEmpty();
        assertThat(fiche.emploi()).isEmpty();
        assertThat(fiche.justificatifs()).isEmpty();
        assertThat(fiche.typesManquants()).containsExactly(TypeDocument.values());
        assertThat(fiche.score().criteres()).hasSize(CritereScore.values().length);
    }

    @Test
    void dossier_inconnu_renvoie_404() {
        assertThatThrownBy(() -> ouvrir(UUID.randomUUID()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void la_fiche_montre_la_decision_et_le_journal_du_dossier() {
        Demande demande = demande(UUID.randomUUID());
        DecisionService decisions = new DecisionsDeTest(demandes, new JournalAuditService(journal), evenement -> { },
                clock).service;
        assertThat(ouvrir(demande.id()).decisionPossible()).isTrue();

        decisions.demanderJustificatif(demande.id(), "analyste", TypeDocument.RIB, null);
        decisions.refuser(demande.id(), "analyste", "Salaire non domicilié");
        VueFicheDossier fiche = ouvrir(demande.id());

        assertThat(fiche.statut()).isEqualTo(StatutDossier.REFUSEE);
        assertThat(fiche.decisionPossible()).isFalse();
        assertThat(fiche.decision()).hasValueSatisfying(d -> {
            assertThat(d.analyste()).isEqualTo("analyste");
            assertThat(d.le()).isEqualTo("12/11/2026 10:00");
            assertThat(d.motif()).contains("Salaire non domicilié");
        });
        assertThat(fiche.journal()).satisfiesExactly(
                e -> {
                    assertThat(e.action()).isEqualTo("DEMANDE_JUSTIFICATIF");
                    assertThat(e.detail()).contains("RIB");
                },
                e -> {
                    assertThat(e.action()).isEqualTo("REFUS");
                    assertThat(e.avant()).contains("EN_ANALYSE");
                    assertThat(e.apres()).contains("REFUSEE");
                    assertThat(e.acteur()).contains("analyste");
                    assertThat(e.typeActeur()).isEqualTo(TypeActeur.BACK_OFFICE);
                    assertThat(e.le()).isEqualTo("12/11/2026 10:00");
                });
    }
}
