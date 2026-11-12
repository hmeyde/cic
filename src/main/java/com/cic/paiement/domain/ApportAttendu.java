package com.cic.paiement.domain;

import java.util.Objects;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Dossier auquel renvoie une référence d'apport : son contrat, le dossier et le client (identifiants
 * d'autres modules) et l'apport fixé au contrat.
 */
public record ApportAttendu(UUID contratId, String numeroContrat, UUID demandeId, UUID clientId, Montant montant) {

    public ApportAttendu {
        Objects.requireNonNull(contratId, "contratId");
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(montant, "montant");
    }
}
