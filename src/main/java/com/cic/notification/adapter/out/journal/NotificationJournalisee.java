package com.cic.notification.adapter.out.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cic.notification.domain.Message;
import com.cic.notification.domain.Notification;

/**
 * Adaptateur du MVP : aucun fournisseur SMS ou WhatsApp, le message est écrit dans les journaux.
 * Le téléphone est masqué ; le texte est journalisé tel quel pour que la démonstration puisse le lire
 * (par exemple un code à usage unique).
 */
@Component
class NotificationJournalisee implements Notification {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationJournalisee.class);

    @Override
    public void envoyer(Message message) {
        LOG.info("Notification {} envoyée à {} : {}", message.canal(), message.telephoneMasque(), message.texte());
    }
}
