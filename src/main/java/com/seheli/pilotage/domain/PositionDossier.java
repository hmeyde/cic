package com.seheli.pilotage.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.seheli.commun.domain.Montant;

/**
 * Dossier qui porte de l'encours, vu par le pilotage.
 *
 * @param restantDu        reste dû par le client
 * @param employeur        employeur du client, tel que déclaré ; vide s'il n'est pas connu
 * @param fournisseur      nom du fournisseur du produit
 * @param plusAncienRetard date de la plus ancienne échéance EN_RETARD ; vide si aucune
 */
public record PositionDossier(UUID demandeId, Montant restantDu, Optional<String> employeur, String fournisseur,
        Optional<LocalDate> plusAncienRetard) {

    public PositionDossier {
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(restantDu, "restantDu");
        Objects.requireNonNull(employeur, "employeur");
        Objects.requireNonNull(fournisseur, "fournisseur");
        Objects.requireNonNull(plusAncienRetard, "plusAncienRetard");
    }
}
