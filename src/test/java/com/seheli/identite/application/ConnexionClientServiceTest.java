package com.seheli.identite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.CodeConnexion.Verification;
import com.seheli.identite.domain.GenerateurCode;
import com.seheli.notification.domain.Message;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class ConnexionClientServiceTest {

    private static final Instant DEBUT = Instant.parse("2026-11-11T09:00:00Z");
    private static final Locale FR = Locale.FRENCH;

    private final AtomicReference<Instant> maintenant = new AtomicReference<>(DEBUT);
    private final Clock clock = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return maintenant.get();
        }
    };

    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final CodesEnMemoire codes = new CodesEnMemoire();
    private final List<Message> envoyes = new ArrayList<>();
    private final AtomicInteger compteur = new AtomicInteger(123456);
    private final GenerateurCode generateur = () -> String.valueOf(compteur.getAndIncrement());
    private ConnexionClientService service;
    private Client client;

    @BeforeEach
    void preparer() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        service = new ConnexionClientService(clients, codes, generateur, new BCryptPasswordEncoder(4), envoyes::add,
                parametres, messages, clock);
        client = Client.nouveau("1234567890", "Ahmed", "Sidi", LocalDate.of(1990, 1, 1), "22123456", DEBUT);
        clients.enregistrer(client);
    }

    private void avancer(Duration duree) {
        maintenant.set(maintenant.get().plus(duree));
    }

    @Test
    void envoie_un_code_par_sms_puis_le_code_connecte_le_client() {
        String telephone = service.demanderCode("+222 22 12 34 56", FR);

        assertThat(telephone).isEqualTo("22123456");
        assertThat(envoyes).singleElement().satisfies(m -> {
            assertThat(m.telephone()).isEqualTo("22123456");
            assertThat(m.texte()).contains("123456").contains("5 minutes");
        });
        assertThat(codes.parTelephone("22123456")).get()
                .satisfies(c -> assertThat(c.codeHash()).startsWith("$2a$").doesNotContain("123456"));

        ResultatConnexion resultat = service.verifierCode(telephone, "123 456");

        assertThat(resultat.estValide()).isTrue();
        assertThat(resultat.clientId()).contains(client.id());
        assertThat(service.verifierCode(telephone, "123456").verification()).isEqualTo(Verification.DEJA_UTILISE);
    }

    @Test
    void le_message_est_redige_dans_la_langue_du_client() {
        service.demanderCode("22123456", Locale.of("ar"));

        assertThat(envoyes.getFirst().texte()).contains("رمز").contains("123456");
    }

    @Test
    void un_numero_sans_compte_recoit_un_code_qui_prouve_le_numero_sans_identifier_de_client() {
        service.demanderCode("33000000", FR);

        assertThat(envoyes).singleElement().satisfies(m -> assertThat(m.telephone()).isEqualTo("33000000"));
        assertThat(service.verifierCode("33000000", "000000").estNouveauClient()).isFalse();
        ResultatConnexion resultat = service.verifierCode("33000000", "123456");

        assertThat(resultat.estValide()).isTrue();
        assertThat(resultat.estNouveauClient()).isTrue();
        assertThat(resultat.clientId()).isEmpty();
    }

    @Test
    void un_client_existant_n_est_pas_un_nouveau_client() {
        service.demanderCode("22123456", FR);

        assertThat(service.verifierCode("22123456", "123456").estNouveauClient()).isFalse();
    }

    @Test
    void sans_code_demande_aucune_connexion() {
        assertThat(service.verifierCode("33000000", "123456").verification()).isEqualTo(Verification.EXPIRE);
    }

    @Test
    void un_numero_mal_forme_est_refuse() {
        assertThatThrownBy(() -> service.demanderCode("1234", FR)).isInstanceOf(TelephoneInvalideException.class);
        assertThatThrownBy(() -> service.demanderCode(null, FR)).isInstanceOf(TelephoneInvalideException.class);
    }

    @Test
    void le_code_n_est_plus_accepte_apres_5_minutes() {
        service.demanderCode("22123456", FR);
        avancer(Duration.ofMinutes(5));

        assertThat(service.verifierCode("22123456", "123456").verification()).isEqualTo(Verification.EXPIRE);
    }

    @Test
    void cinq_essais_faux_bloquent_le_code() {
        service.demanderCode("22123456", FR);
        for (int i = 0; i < 4; i++) {
            assertThat(service.verifierCode("22123456", "000000").verification()).isEqualTo(Verification.INCORRECT);
        }
        assertThat(service.verifierCode("22123456", "000000").verification())
                .isEqualTo(Verification.ESSAIS_EPUISES);

        assertThat(service.verifierCode("22123456", "123456").estValide()).isFalse();
    }

    @Test
    void pas_de_nouveau_code_avant_60_secondes_si_le_code_en_cours_est_utilisable() {
        service.demanderCode("22123456", FR);
        avancer(Duration.ofSeconds(30));
        service.demanderCode("22123456", FR);

        assertThat(envoyes).hasSize(1);
        assertThat(service.verifierCode("22123456", "123456").estValide()).isTrue();
    }

    @Test
    void un_nouveau_code_remplace_le_precedent() {
        service.demanderCode("22123456", FR);
        avancer(Duration.ofSeconds(61));
        service.demanderCode("22123456", FR);

        assertThat(envoyes).hasSize(2);
        assertThat(service.verifierCode("22123456", "123456").estValide()).isFalse();
        assertThat(service.verifierCode("22123456", "123457").estValide()).isTrue();
    }

    @Test
    void un_code_bloque_peut_etre_remplace_sans_attendre() {
        service.demanderCode("22123456", FR);
        for (int i = 0; i < 5; i++) {
            service.verifierCode("22123456", "000000");
        }
        service.demanderCode("22123456", FR);

        assertThat(envoyes).hasSize(2);
        assertThat(service.verifierCode("22123456", "123457").estValide()).isTrue();
    }
}
