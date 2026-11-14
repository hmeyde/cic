package com.cic.contrat.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.ContenuContrat;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Prix;
import com.cic.identite.application.ClientService;
import com.cic.identite.application.ClientsEnMemoire;
import com.cic.identite.domain.Client;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class ContratPdfServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant ACCEPTE_LE = Instant.parse("2026-11-13T09:30:00Z");

    private final ContratsEnMemoire contrats = new ContratsEnMemoire();
    private final AutorisationsEnMemoire autorisations = new AutorisationsEnMemoire();
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final AtomicReference<ContenuContrat> rendu = new AtomicReference<>();
    private final ContratService contratService;
    private final ContratPdfService service;
    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Mohamed", LocalDate.of(1990, 5, 1),
            "22123456", ACCEPTE_LE);

    ContratPdfServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        Clock clock = Clock.fixed(ACCEPTE_LE, NOUAKCHOTT);
        contratService = new ContratService(contrats, autorisations, parametres, clock);
        service = new ContratPdfService(contrats, new ClientService(clients), parametres, contenu -> {
            rendu.set(contenu);
            return new byte[] {'%', 'P', 'D', 'F'};
        }, clock);
        clients.enregistrer(client);
    }

    private Contrat etablir(UUID demandeId) {
        return contratService.etablir(new ContratAEtablir(demandeId, client.id(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), 6, 25, Optional.empty(), ACCEPTE_LE));
    }

    @Test
    void imprime_le_contrat_du_dossier_avec_l_acheteur_et_la_date_limite_de_l_apport() {
        UUID demandeId = UUID.randomUUID();
        Contrat contrat = etablir(demandeId);

        Optional<ContratPdf> pdf = service.pourDemande(demandeId);

        assertThat(pdf).hasValueSatisfying(p -> {
            assertThat(p.nomFichier()).isEqualTo("contrat-SEH-2026-000001.pdf");
            assertThat(p.contenu()).startsWith('%', 'P', 'D', 'F');
        });
        ContenuContrat contenu = rendu.get();
        assertThat(contenu.contrat()).isEqualTo(contrat);
        assertThat(contenu.etabliLe()).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(contenu.dateLimiteApport()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(contenu.acheteur().nom()).isEqualTo("Ould Ahmed");
        assertThat(contenu.acheteur().prenom()).isEqualTo("Mohamed");
        assertThat(contenu.acheteur().nni()).isEqualTo("1234567890");
        assertThat(contenu.raisonSociale()).isEqualTo("Seheli");
    }

    @Test
    void pas_de_pdf_sans_contrat() {
        assertThat(service.pourDemande(UUID.randomUUID())).isEmpty();
        assertThat(rendu.get()).isNull();
    }
}
