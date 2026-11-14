package com.seheli.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

class LanguesLocaleResolverTest {

    private final LocaleResolver resolver = new WebConfig().localeResolver();

    @Test
    void francais_par_defaut_meme_si_le_navigateur_demande_une_autre_langue() {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.addPreferredLocale(Locale.ENGLISH);

        assertThat(resolver.resolveLocale(requete)).isEqualTo(Locale.FRENCH);
    }

    @Test
    void choisir_l_arabe_le_memorise_dans_un_cookie() {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        MockHttpServletResponse reponse = new MockHttpServletResponse();

        resolver.setLocale(requete, reponse, Locale.of("ar"));

        Cookie cookie = reponse.getCookie("seheli_langue");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();

        MockHttpServletRequest suivante = new MockHttpServletRequest();
        suivante.setCookies(cookie);
        assertThat(resolver.resolveLocale(suivante)).isEqualTo(Locale.of("ar"));
    }

    @Test
    void une_variante_regionale_est_ramenee_a_sa_langue() {
        assertThat(LanguesLocaleResolver.normaliser(Locale.of("ar", "MR"))).isEqualTo(Locale.of("ar"));
        assertThat(LanguesLocaleResolver.normaliser(Locale.of("fr", "FR"))).isEqualTo(Locale.FRENCH);
    }

    @Test
    void une_langue_non_prise_en_charge_est_ramenee_au_francais() {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        MockHttpServletResponse reponse = new MockHttpServletResponse();

        resolver.setLocale(requete, reponse, Locale.ENGLISH);

        assertThat(resolver.resolveLocale(requete)).isEqualTo(Locale.FRENCH);
        assertThat(LanguesLocaleResolver.normaliser(null)).isEqualTo(Locale.FRENCH);
    }
}
