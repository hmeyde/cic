package com.cic.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void un_sms_porte_le_telephone_le_canal_et_le_texte() {
        Message m = Message.sms("36123456", "Bonjour");

        assertThat(m.telephone()).isEqualTo("36123456");
        assertThat(m.canal()).isEqualTo(Canal.SMS);
        assertThat(m.texte()).isEqualTo("Bonjour");
        assertThat(Message.whatsapp("36123456", "Bonjour").canal()).isEqualTo(Canal.WHATSAPP);
    }

    @Test
    void le_telephone_doit_etre_normalise_sur_huit_chiffres() {
        assertThatThrownBy(() -> Message.sms("+222 36 12 34 56", "Bonjour"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Message.sms("3612345", "Bonjour")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_texte_ne_peut_pas_etre_vide() {
        assertThatThrownBy(() -> Message.sms("36123456", "  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_telephone_masque_et_le_toString_ne_revelent_ni_le_numero_ni_le_contenu() {
        Message m = Message.sms("36123456", "Votre code : 123456");

        assertThat(m.telephoneMasque()).isEqualTo("****3456");
        assertThat(m.toString()).doesNotContain("3612").doesNotContain("123456").contains("****3456");
    }
}
