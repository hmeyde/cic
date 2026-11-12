package com.cic.demande;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Événement publié quand le paiement fournisseur d'un dossier est validé et son bon de commande
 * émis, par exemple pour l'écriture comptable du paiement fournisseur. Aucune donnée personnelle :
 * seulement des identifiants et le montant dû au fournisseur.
 */
public record CommandeFournisseurValidee(UUID commandeId, String numero, UUID demandeId, UUID contratId,
        UUID fournisseurId, Montant montant, Instant le) {

    public CommandeFournisseurValidee {
        Objects.requireNonNull(commandeId, "commandeId");
        Objects.requireNonNull(numero, "numero");
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(contratId, "contratId");
        Objects.requireNonNull(fournisseurId, "fournisseurId");
        Objects.requireNonNull(montant, "montant");
        Objects.requireNonNull(le, "le");
    }
}
