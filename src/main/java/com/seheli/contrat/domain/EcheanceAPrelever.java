package com.seheli.contrat.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Échéance à prélever, avec ce qu'il faut savoir de son contrat pour prévenir le client : numéro,
 * dossier, client (par identifiant) et nombre total d'échéances.
 */
public record EcheanceAPrelever(UUID contratId, String numeroContrat, UUID demandeId, UUID clientId,
        int nombreEcheances, Echeance echeance) {

    public EcheanceAPrelever {
        Objects.requireNonNull(contratId, "contratId");
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(echeance, "echeance");
    }

    public static EcheanceAPrelever de(Contrat contrat, Echeance echeance) {
        return new EcheanceAPrelever(contrat.id(), contrat.numero(), contrat.demandeId(), contrat.clientId(),
                contrat.nombreMois(), echeance);
    }
}
