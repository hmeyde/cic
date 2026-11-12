package com.cic.demande.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import com.cic.catalogue.application.CatalogueClientService;
import com.cic.catalogue.application.FournisseursEnMemoire;
import com.cic.catalogue.application.ProduitsEnMemoire;
import com.cic.commun.domain.Montant;
import com.cic.contrat.application.CalculPrixService;
import com.cic.contrat.application.EcheancierService;
import com.cic.contrat.application.SimulationService;
import com.cic.demande.application.DemandeService;
import com.cic.demande.application.DemandesEnMemoire;
import com.cic.demande.application.EligibiliteService;
import com.cic.demande.application.ScoreService;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandesFixtures;
import com.cic.demande.domain.StatutDossier;
import com.cic.identite.application.ClientService;
import com.cic.identite.application.ClientsEnMemoire;
import com.cic.identite.application.EmploiClientService;
import com.cic.identite.application.EmploisEnMemoire;
import com.cic.identite.domain.Client;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class FileAnalyseBackOfficeControllerTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");

    private final Clock clock = Clock.fixed(LocalDate.of(2026, 11, 12).atTime(10, 0).atZone(NOUAKCHOTT).toInstant(),
            NOUAKCHOTT);
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final FileAnalyseBackOfficeController controller;

    FileAnalyseBackOfficeControllerTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        CalculPrixService calculPrix = new CalculPrixService(parametres);
        EcheancierService echeancier = new EcheancierService(parametres);
        DemandeService service = new DemandeService(demandes, new ClientService(clients),
                new EmploiClientService(clients, new EmploisEnMemoire(), clock),
                new CatalogueClientService(new ProduitsEnMemoire(), new FournisseursEnMemoire(), calculPrix,
                        new SimulationService(calculPrix, echeancier), parametres),
                echeancier, new ScoreService(parametres, new EligibiliteService(parametres, clock), clock),
                parametres, evenement -> { }, clock);
        controller = new FileAnalyseBackOfficeController(service, clock);
    }

    private Client client(String nni, String nom, String prenom) {
        Client c = Client.nouveau(nni, nom, prenom, LocalDate.of(1990, 5, 1), "2212" + nni.substring(6),
                clock.instant());
        clients.enregistrer(c);
        return c;
    }

    private Demande demande(UUID clientId, StatutDossier statut, Duration ilYA) {
        Instant le = clock.instant().minus(ilYA);
        Demande d = Demande.reconstituer(UUID.randomUUID(), clientId, DemandesFixtures.termes(),
                DemandesFixtures.score(), le, statut, le);
        demandes.enregistrer(d);
        return d;
    }

    @SuppressWarnings("unchecked")
    private List<LigneFileAnalyse> file() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.file(model)).isEqualTo("bo/dossiers/file");
        return (List<LigneFileAnalyse>) model.get("dossiers");
    }

    @Test
    void file_des_dossiers_en_analyse_du_plus_ancien_au_plus_recent() {
        Client aicha = client("1000000001", "Mint Sidi", "Aïcha");
        Client mohamed = client("1000000002", "Ould Ahmed", "Mohamed");
        Client fatimetou = client("1000000003", "Mint Brahim", "Fatimetou");
        Demande recente = demande(aicha.id(), StatutDossier.EN_ANALYSE, Duration.ofHours(2));
        Demande ancienne = demande(mohamed.id(), StatutDossier.EN_ANALYSE, Duration.ofDays(3).plusHours(1));
        demande(fatimetou.id(), StatutDossier.ACCEPTEE, Duration.ofDays(10));
        demande(fatimetou.id(), StatutDossier.REFUSEE, Duration.ofDays(20));

        List<LigneFileAnalyse> file = file();

        assertThat(file).extracting(LigneFileAnalyse::id).containsExactly(ancienne.id(), recente.id());
        assertThat(file).extracting(LigneFileAnalyse::rang).containsExactly(1, 2);
        assertThat(file.getFirst()).satisfies(l -> {
            assertThat(l.client()).isEqualTo("Mohamed Ould Ahmed");
            assertThat(l.attenteJours()).isEqualTo(3);
            assertThat(l.soumiseLe()).isEqualTo("09/11/2026 09:00");
            assertThat(l.designation()).isEqualTo("Galaxy A35");
            assertThat(l.prixVente()).isEqualTo(Montant.de(20_800));
            assertThat(l.nombreMois()).isEqualTo(6);
            assertThat(l.mensualite()).isEqualTo(Montant.de(2_080));
            assertThat(l.note()).isEqualTo(DemandesFixtures.score().note().note());
            assertThat(l.recommandation()).isEqualTo(DemandesFixtures.score().recommandation());
        });
        assertThat(file.get(1).attenteJours()).isZero();
    }

    @Test
    void file_vide() {
        demande(UUID.randomUUID(), StatutDossier.ACCEPTEE, Duration.ofDays(1));

        assertThat(file()).isEmpty();
    }

    @Test
    void client_introuvable_affiche_un_nom_vide() {
        demande(UUID.randomUUID(), StatutDossier.EN_ANALYSE, Duration.ofDays(1));

        assertThat(file()).singleElement().satisfies(l -> assertThat(l.client()).isEmpty());
    }
}
