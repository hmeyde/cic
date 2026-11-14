package com.seheli.identite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;
import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.Emploi;
import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeContrat;

class EmploiClientServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-11T09:00:00Z");
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 11);

    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final EmploisEnMemoire emplois = new EmploisEnMemoire();
    private final EmploiClientService service = new EmploiClientService(clients, emplois,
            Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott")));
    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Sidi", LocalDate.of(1990, 1, 15),
            "22123456", MAINTENANT);

    EmploiClientServiceTest() {
        clients.enregistrer(client);
    }

    static SaisieEmploi cdi(String employeur, long salaire) {
        return new SaisieEmploi(employeur, TypeContrat.CDI, LocalDate.of(2022, 3, 1), null, Montant.de(salaire), 25,
                true, true, ModeRemboursement.DOMICILIATION_BMCI);
    }

    private static SaisieEmploi cdd(LocalDate debut, LocalDate fin) {
        return new SaisieEmploi("SNIM", TypeContrat.CDD, debut, fin, Montant.de(20_000), 28, true, false,
                ModeRemboursement.VIREMENT_PERMANENT);
    }

    @Test
    void enregistre_l_emploi_du_client() {
        Emploi emploi = service.enregistrer(client.id(), cdi("SNIM", 30_000));

        assertThat(service.parClient(client.id())).contains(emploi);
        assertThat(emploi.clientId()).isEqualTo(client.id());
        assertThat(emploi.salaireNet()).isEqualTo(Montant.de(30_000));
        assertThat(emploi.jourPaie()).isEqualTo(25);
        assertThat(emploi.modeRemboursement()).isEqualTo(ModeRemboursement.DOMICILIATION_BMCI);
    }

    @Test
    void une_seconde_saisie_modifie_l_emploi_existant() {
        Emploi premier = service.enregistrer(client.id(), cdi("SNIM", 30_000));

        Emploi modifie = service.enregistrer(client.id(), cdd(LocalDate.of(2025, 1, 1), LocalDate.of(2027, 12, 31)));

        assertThat(modifie.id()).isEqualTo(premier.id());
        Emploi lu = service.parClient(client.id()).orElseThrow();
        assertThat(lu.employeur()).isEqualTo("SNIM");
        assertThat(lu.typeContrat()).isEqualTo(TypeContrat.CDD);
        assertThat(lu.dateFin()).contains(LocalDate.of(2027, 12, 31));
        assertThat(lu.salaireNet()).isEqualTo(Montant.de(20_000));
        assertThat(lu.modeRemboursement()).isEqualTo(ModeRemboursement.VIREMENT_PERMANENT);
    }

    @Test
    void refuse_un_emploi_qui_n_est_pas_en_cours() {
        assertThatThrownBy(() -> service.enregistrer(client.id(), cdd(AUJOURDHUI.plusDays(1), AUJOURDHUI.plusYears(1))))
                .isInstanceOf(DateDebutEmploiFutureException.class);
        assertThatThrownBy(() -> service.enregistrer(client.id(), cdd(LocalDate.of(2024, 1, 1), AUJOURDHUI.minusDays(1))))
                .isInstanceOf(ContratTermineException.class);
        assertThat(service.parClient(client.id())).isEmpty();

        service.enregistrer(client.id(), cdd(AUJOURDHUI, AUJOURDHUI.plusDays(1)));
        service.enregistrer(client.id(), cdd(LocalDate.of(2024, 1, 1), AUJOURDHUI));
        assertThat(service.parClient(client.id())).isPresent();
    }

    @Test
    void refuse_un_client_inconnu() {
        assertThatThrownBy(() -> service.enregistrer(UUID.randomUUID(), cdi("SNIM", 30_000)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
