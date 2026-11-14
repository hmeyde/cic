package com.seheli.notification.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Message à envoyer à un client.
 *
 * @param telephone numéro mauritanien normalisé sur 8 chiffres (sans +222), comme dans le module identite
 * @param canal     canal d'envoi
 * @param texte     contenu du message, déjà rédigé dans la langue du client
 */
public record Message(String telephone, Canal canal, String texte) {

    private static final Pattern TELEPHONE = Pattern.compile("\\d{8}");

    public Message {
        Objects.requireNonNull(telephone, "telephone");
        Objects.requireNonNull(canal, "canal");
        Objects.requireNonNull(texte, "texte");
        if (!TELEPHONE.matcher(telephone).matches()) {
            throw new IllegalArgumentException("Le téléphone doit contenir 8 chiffres");
        }
        if (texte.isBlank()) {
            throw new IllegalArgumentException("Le texte du message est vide");
        }
    }

    public static Message sms(String telephone, String texte) {
        return new Message(telephone, Canal.SMS, texte);
    }

    public static Message whatsapp(String telephone, String texte) {
        return new Message(telephone, Canal.WHATSAPP, texte);
    }

    /** Téléphone masqué, seule forme autorisée dans les journaux : {@code ****3456}. */
    public String telephoneMasque() {
        return "****" + telephone.substring(telephone.length() - 4);
    }

    /** Sans donnée personnelle : ni téléphone complet ni contenu. */
    @Override
    public String toString() {
        return "Message[canal=" + canal + ", telephone=" + telephoneMasque() + "]";
    }
}
