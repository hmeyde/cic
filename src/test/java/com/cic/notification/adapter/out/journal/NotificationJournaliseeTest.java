package com.cic.notification.adapter.out.journal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.cic.notification.domain.Message;

@ExtendWith(OutputCaptureExtension.class)
class NotificationJournaliseeTest {

    @Test
    void un_message_envoye_apparait_dans_les_journaux_avec_le_telephone_masque(CapturedOutput sortie) {
        new NotificationJournalisee().envoyer(Message.sms("36123456", "Votre code CIC : 482913"));

        assertThat(sortie.getOut())
                .contains("Notification SMS envoyée à ****3456 : Votre code CIC : 482913")
                .doesNotContain("36123456");
    }
}
