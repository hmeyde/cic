package com.seheli.recouvrement.domain;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Lien {@code https://wa.me/} qui ouvre une conversation WhatsApp avec Seheli, le message étant déjà
 * saisi : le client n'a plus qu'à l'envoyer. Le numéro mauritanien (8 chiffres) reçoit l'indicatif
 * 222 ; le texte est encodé pour une URL (espaces en {@code %20}, que WhatsApp comprend partout).
 */
public final class LienWhatsApp {

    private static final Pattern TELEPHONE = Pattern.compile("\\d{8}");
    private static final String INDICATIF_MAURITANIE = "222";

    private LienWhatsApp() {
    }

    /**
     * @param telephone numéro WhatsApp à joindre, 8 chiffres sans indicatif
     * @param texte     message prérempli
     */
    public static String vers(String telephone, String texte) {
        Objects.requireNonNull(telephone, "telephone");
        Objects.requireNonNull(texte, "texte");
        String numero = telephone.replace(" ", "");
        if (!TELEPHONE.matcher(numero).matches()) {
            throw new IllegalArgumentException("Numéro WhatsApp invalide : 8 chiffres attendus");
        }
        if (texte.isBlank()) {
            throw new IllegalArgumentException("Le message prérempli est vide");
        }
        return "https://wa.me/" + INDICATIF_MAURITANIE + numero + "?text="
                + URLEncoder.encode(texte, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
