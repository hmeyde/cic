package com.cic.contrat.domain;

import java.util.UUID;

/**
 * Contenu des autorisations signées (port de sortie), rangé sous l'identifiant du document, hors du
 * dossier public de l'application, servi seulement à travers les contrôles d'accès du back-office.
 */
public interface StockageDocumentsSignes {

    void ecrire(UUID documentId, byte[] contenu);

    /** @throws IllegalStateException contenu introuvable ou illisible */
    byte[] lire(UUID documentId);

    /** Supprime le contenu s'il existe (sans erreur sinon). */
    void supprimer(UUID documentId);
}
