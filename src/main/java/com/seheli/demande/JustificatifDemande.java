package com.seheli.demande;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.seheli.identite.domain.TypeDocument;

/**
 * Événement publié quand l'analyste demande un justificatif au client, pour le prévenir. Aucune
 * donnée personnelle : seulement des identifiants et la nature de la pièce.
 */
public record JustificatifDemande(UUID demandeId, UUID clientId, TypeDocument type, Instant le) {

    public JustificatifDemande {
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(le, "le");
    }
}
