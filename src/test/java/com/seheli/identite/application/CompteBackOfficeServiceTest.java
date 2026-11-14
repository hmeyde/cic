package com.seheli.identite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.seheli.identite.domain.RoleBackOffice;
import com.seheli.identite.domain.UtilisateurBackOffice;

class CompteBackOfficeServiceTest {

    private final BCryptPasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private final UtilisateursEnMemoire repository = new UtilisateursEnMemoire();
    private final CompteBackOfficeService service = new CompteBackOfficeService(repository, encodeur);

    @Test
    void le_mot_de_passe_est_stocke_hache_en_bcrypt() {
        UtilisateurBackOffice u = service.creer("Admin", "motdepasse-solide", RoleBackOffice.ADMIN);

        assertThat(u.identifiant()).isEqualTo("admin");
        assertThat(u.motDePasseHash()).startsWith("$2a$").isNotEqualTo("motdepasse-solide");
        assertThat(encodeur.matches("motdepasse-solide", u.motDePasseHash())).isTrue();
        assertThat(repository.parIdentifiant("admin")).contains(u);
    }

    @Test
    void un_identifiant_deja_utilise_est_refuse_quelle_que_soit_la_casse() {
        service.creer("admin", "motdepasse-solide", RoleBackOffice.ADMIN);

        assertThatThrownBy(() -> service.creer("ADMIN", "autre-mot-de-passe", RoleBackOffice.ANALYSTE))
                .isInstanceOf(IdentifiantDejaUtiliseException.class);
    }

    @Test
    void un_mot_de_passe_trop_court_ou_trop_long_est_refuse() {
        assertThatThrownBy(() -> service.creer("admin", "court", RoleBackOffice.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.creer("admin", "x".repeat(73), RoleBackOffice.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.nombre()).isZero();
    }

    @Test
    void creer_si_absent_ne_modifie_pas_un_compte_existant() {
        UtilisateurBackOffice existant = service.creer("admin", "premier-mot-de-passe", RoleBackOffice.ADMIN);

        boolean cree = service.creerSiAbsent("admin", "second-mot-de-passe", RoleBackOffice.ADMIN);

        assertThat(cree).isFalse();
        assertThat(repository.parIdentifiant("admin")).get()
                .extracting(UtilisateurBackOffice::motDePasseHash).isEqualTo(existant.motDePasseHash());
    }

    @Test
    void la_recherche_ignore_la_casse_et_les_identifiants_mal_formes() {
        service.creer("analyste1", "motdepasse-solide", RoleBackOffice.ANALYSTE);

        assertThat(service.parIdentifiant(" Analyste1 ")).isPresent();
        assertThat(service.parIdentifiant("a b")).isEmpty();
    }
}
