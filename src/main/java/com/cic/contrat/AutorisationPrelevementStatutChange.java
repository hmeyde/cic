package com.cic.contrat;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.cic.contrat.domain.StatutAutorisation;

/**
 * Événement publié à chaque changement de statut d'une autorisation de prélèvement, par exemple pour
 * faire passer le dossier à APPORT_ET_AUTORISATION quand l'autorisation est ACCEPTEE et l'apport
 * encaissé. Aucune donnée personnelle : seulement des identifiants.
 */
public record AutorisationPrelevementStatutChange(UUID autorisationId, UUID demandeId, UUID clientId,
        StatutAutorisation depuis, StatutAutorisation vers, Instant le) {

    public AutorisationPrelevementStatutChange {
        Objects.requireNonNull(autorisationId, "autorisationId");
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(depuis, "depuis");
        Objects.requireNonNull(vers, "vers");
        Objects.requireNonNull(le, "le");
    }
}
