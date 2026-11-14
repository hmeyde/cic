package com.seheli.paiement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.seheli.commun.domain.Montant;

/**
 * Événement publié quand un paiement reçu ne peut être rattaché à aucun dossier (référence inconnue) :
 * l'argent est encaissé et doit être comptabilisé en attente d'identification. Aucune donnée
 * personnelle : seulement l'identifiant et le montant.
 */
public record PaiementNonRapproche(UUID paiementId, Montant montant, LocalDate recuLe, Instant le) {

    public PaiementNonRapproche {
        Objects.requireNonNull(paiementId, "paiementId");
        Objects.requireNonNull(montant, "montant");
        Objects.requireNonNull(recuLe, "recuLe");
        Objects.requireNonNull(le, "le");
    }
}
