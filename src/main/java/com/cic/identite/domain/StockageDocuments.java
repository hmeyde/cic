package com.cic.identite.domain;

import java.util.UUID;

/**
 * Contenu des justificatifs (port de sortie). Le contenu est rangé sous l'identifiant du document,
 * hors du dossier public de l'application, et n'est servi qu'à travers les contrôles d'accès.
 */
public interface StockageDocuments {

    void ecrire(UUID documentId, byte[] contenu);

    /** @throws IllegalStateException contenu introuvable ou illisible */
    byte[] lire(UUID documentId);

    /** Supprime le contenu s'il existe (sans erreur sinon). */
    void supprimer(UUID documentId);
}
