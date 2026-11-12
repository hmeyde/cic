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
import com.cic.contrat.domain.ContenuAutorisation;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Prix;
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

class AutorisationPdfServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant ACCEPTE_LE = Instant.parse("2026-11-13T09:30:00Z");

    private final ContratsEnMemoire contrats = new ContratsEnMemoire();
    private final AutorisationsEnMemoire autorisations = new AutorisationsEnMemoire();
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final EmploisEnMemoire emplois = new EmploisEnMemoire();
    private final AtomicReference<ContenuAutorisation> rendu = new AtomicReference<>();
    private final ContratService contratService;
    private final AutorisationPdfService service;
    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Mohamed", LocalDate.of(1990, 5, 1),
            "22123456", ACCEPTE_LE);

    AutorisationPdfServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        Clock clock = Clock.fixed(ACCEPTE_LE, NOUAKCHOTT);
        contratService = new ContratService(contrats, autorisations, parametres, clock);
        service = new AutorisationPdfService(contrats, autorisations, new ClientService(clients),
                new EmploiClientService(clients, emplois, clock), parametres, contenu -> {
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

    private void emploi(boolean domicilieBmci) {
        emplois.enregistrer(Emploi.nouveau(client.id(), "SNIM", TypeContrat.CDI, LocalDate.of(2020, 1, 1), null,
                Montant.de(30_000), 25, true, domicilieBmci, ModeRemboursement.VIREMENT_PERMANENT));
    }

    @Test
    void imprime_l_autorisation_remplie_avec_le_compte_de_cic_et_la_date_limite() {
        emploi(false);
        UUID demandeId = UUID.randomUUID();
        Contrat contrat = etablir(demandeId);

        Optional<ContratPdf> pdf = service.pourDemande(demandeId);

        assertThat(pdf).hasValueSatisfying(p -> {
            assertThat(p.nomFichier()).isEqualTo("autorisation-prelevement-CIC-2026-000001.pdf");
            assertThat(p.contenu()).startsWith('%', 'P', 'D', 'F');
        });
        ContenuAutorisation contenu = rendu.get();
        assertThat(contenu.contrat()).isEqualTo(contrat);
        assertThat(contenu.autorisation().contratId()).isEqualTo(contrat.id());
        assertThat(contenu.titulaire().nni()).isEqualTo("1234567890");
        assertThat(contenu.beneficiaire().titulaire()).isEqualTo("CIC SARL");
        assertThat(contenu.beneficiaire().banque()).isEqualTo("BMCI");
        assertThat(contenu.beneficiaire().numero()).isEqualTo("00020 00101 00001234567 53");
        assertThat(contenu.banqueTitulaire()).isEmpty();
        assertThat(contenu.emiseLe()).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(contenu.dateLimite()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void salaire_domicilie_a_la_bmci_la_banque_du_client_est_remplie() {
        emploi(true);
        UUID demandeId = UUID.randomUUID();
        etablir(demandeId);

        service.pourDemande(demandeId);

        assertThat(rendu.get().banqueTitulaire()).contains("BMCI");
    }

    @Test
    void pas_de_pdf_sans_contrat() {
        assertThat(service.pourDemande(UUID.randomUUID())).isEmpty();
        assertThat(rendu.get()).isNull();
    }
}
