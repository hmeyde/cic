package com.cic.demande;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.cic.demande.domain.StatutDossier;

/**
 * Événement publié à chaque changement de statut d'un dossier, y compris sa création, pour le
 * journal d'audit et les notifications. Aucune donnée personnelle : seulement des identifiants.
 *
 * @param depuis statut précédent ; {@code null} à la création du dossier
 */
public record StatutDossierChange(UUID demandeId, UUID clientId, StatutDossier depuis, StatutDossier vers,
        Instant le) {

    public StatutDossierChange {
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(vers, "vers");
        Objects.requireNonNull(le, "le");
    }
}
