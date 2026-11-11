package com.cic.identite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.cic.identite.domain.Client;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class CreationCompteClientServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-11T09:00:00Z");
    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final CreationCompteClientService service = new CreationCompteClientService(clients, parametres(), clock);

    private static ParametreService parametres() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        return parametres;
    }

    private static NouveauCompteClient saisie(String nni, LocalDate naissance, boolean consentement) {
        return new NouveauCompteClient("22123456", nni, " Ould Ahmed ", "Sidi", naissance, consentement);
    }

    @Test
    void cree_le_compte_avec_le_telephone_prouve_et_l_instant_du_consentement() {
        Client client = service.creer(saisie("12345 67890", LocalDate.of(1990, 1, 15), true));

        assertThat(clients.parId(client.id())).get().satisfies(c -> {
            assertThat(c.telephone()).isEqualTo("22123456");
            assertThat(c.nni()).isEqualTo("1234567890");
            assertThat(c.nom()).isEqualTo("Ould Ahmed");
            assertThat(c.prenom()).isEqualTo("Sidi");
            assertThat(c.dateNaissance()).isEqualTo(LocalDate.of(1990, 1, 15));
            assertThat(c.consentementDonneesLe()).isEqualTo(MAINTENANT);
            assertThat(c.estNniVerifie()).isFalse();
            assertThat(c.estPieceIdentiteVerifiee()).isFalse();
        });
    }

    @Test
    void refuse_sans_consentement_explicite() {
        assertThatThrownBy(() -> service.creer(saisie("1234567890", LocalDate.of(1990, 1, 15), false)))
                .isInstanceOf(ConsentementManquantException.class);
        assertThat(clients.parNni("1234567890")).isEmpty();
    }

    @Test
    void refuse_un_client_de_moins_de_18_ans() {
        // 18 ans le 12 novembre 2026 : encore 17 ans le 11.
        assertThatThrownBy(() -> service.creer(saisie("1234567890", LocalDate.of(2008, 11, 12), true)))
                .isInstanceOfSatisfying(AgeMinimumNonAtteintException.class,
                        e -> assertThat(e.ageMinimum()).isEqualTo(18));
        assertThat(service.creer(saisie("1234567890", LocalDate.of(2008, 11, 11), true))).isNotNull();
    }

    @Test
    void refuse_une_date_de_naissance_future_ou_invraisemblable() {
        assertThatThrownBy(() -> service.creer(saisie("1234567890", LocalDate.of(2026, 11, 12), true)))
                .isInstanceOf(DateNaissanceInvalideException.class);
        assertThatThrownBy(() -> service.creer(saisie("1234567890", LocalDate.of(1900, 1, 1), true)))
                .isInstanceOf(DateNaissanceInvalideException.class);
    }

    @Test
    void un_nni_deja_utilise_est_refuse() {
        clients.enregistrer(Client.nouveau("1234567890", "A", "B", LocalDate.of(1980, 1, 1), "33000000", MAINTENANT));

        assertThatThrownBy(() -> service.creer(saisie("1234567890", LocalDate.of(1990, 1, 15), true)))
                .isInstanceOf(NniDejaUtiliseException.class);
    }

    @Test
    void un_seul_compte_par_telephone() {
        service.creer(saisie("1234567890", LocalDate.of(1990, 1, 15), true));

        assertThatThrownBy(() -> service.creer(saisie("1111111111", LocalDate.of(1990, 1, 15), true)))
                .isInstanceOf(CompteDejaExistantException.class);
    }

    @Test
    void un_nni_mal_forme_est_refuse() {
        assertThatThrownBy(() -> service.creer(saisie("12345", LocalDate.of(1990, 1, 15), true)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
