package com.seheli.config;

import java.util.Locale;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.LocaleResolver;

/**
 * Ne laisse passer que les langues de l'interface (français, arabe).
 * Toute autre locale (cookie modifié, {@code ?lang=en}, {@code ar_MR}...) est ramenée
 * à sa langue si elle est prise en charge, sinon au français.
 */
public class LanguesLocaleResolver implements LocaleResolver {

    public static final Locale FRANCAIS = Locale.FRENCH;
    public static final Locale ARABE = Locale.of("ar");
    static final Set<Locale> LANGUES = Set.of(FRANCAIS, ARABE);

    private final LocaleResolver delegue;

    public LanguesLocaleResolver(LocaleResolver delegue) {
        this.delegue = delegue;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return normaliser(delegue.resolveLocale(request));
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        delegue.setLocale(request, response, locale == null ? null : normaliser(locale));
    }

    static Locale normaliser(Locale locale) {
        if (locale == null) {
            return FRANCAIS;
        }
        Locale langue = Locale.of(locale.getLanguage());
        return LANGUES.contains(langue) ? langue : FRANCAIS;
    }
}
