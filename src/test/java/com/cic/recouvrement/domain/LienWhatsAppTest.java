package com.cic.recouvrement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LienWhatsAppTest {

    @Test
    void le_lien_vise_le_numero_avec_l_indicatif_222_et_encode_le_texte() {
        assertThat(LienWhatsApp.vers("20 00 00 00", "Bonjour CIC, j'ai une question (contrat CIC-2026-000042) ?"))
                .isEqualTo("https://wa.me/22220000000?text=Bonjour%20CIC%2C%20j%27ai%20une%20question%20"
                        + "%28contrat%20CIC-2026-000042%29%20%3F");
    }

    @Test
    void les_accents_et_l_arabe_sont_encodes_en_utf8() {
        assertThat(LienWhatsApp.vers("20000000", "mensualité")).endsWith("?text=mensualit%C3%A9");
        assertThat(LienWhatsApp.vers("20000000", "قسط")).endsWith("?text=%D9%82%D8%B3%D8%B7");
    }

    @Test
    void un_numero_mal_forme_ou_un_texte_vide_est_refuse() {
        assertThatThrownBy(() -> LienWhatsApp.vers("+22220000000", "Bonjour"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LienWhatsApp.vers("20000000", " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
