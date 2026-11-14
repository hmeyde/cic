package com.seheli.notification.domain;

/**
 * Port d'envoi des messages au client (SMS ou WhatsApp).
 * Dans le MVP, l'implémentation journalise le message au lieu de l'envoyer.
 */
public interface Notification {

    void envoyer(Message message);
}
