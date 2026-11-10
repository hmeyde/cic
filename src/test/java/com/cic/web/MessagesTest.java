package com.cic.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/** Les fichiers de messages français et arabe doivent rester alignés. */
class MessagesTest {

    @Test
    void le_francais_et_l_arabe_ont_les_memes_cles() throws IOException {
        Properties francais = charger("messages.properties");
        Properties arabe = charger("messages_ar.properties");

        assertThat(arabe.stringPropertyNames())
                .containsExactlyInAnyOrderElementsOf(francais.stringPropertyNames());
    }

    @Test
    void aucun_message_n_est_vide() throws IOException {
        for (String fichier : new String[] {"messages.properties", "messages_ar.properties"}) {
            Properties messages = charger(fichier);
            messages.stringPropertyNames().forEach(cle ->
                    assertThat(messages.getProperty(cle)).as(fichier + " : " + cle).isNotBlank());
        }
    }

    private static Properties charger(String fichier) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = MessagesTest.class.getClassLoader().getResourceAsStream(fichier)) {
            assertThat(in).as(fichier).isNotNull();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        return properties;
    }
}
