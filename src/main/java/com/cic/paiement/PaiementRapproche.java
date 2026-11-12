package com.cic.paiement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Événement publié quand un paiement reçu est rattaché à un dossier par sa référence d'apport, par
 * exemple pour faire passer le dossier à APPORT_ET_AUTORISATION quand l'apport est encaissé et
 * l'autorisation ACCEPTEE. Aucune donnée personnelle : seulement des identifiants et des montants.
 *
 * @param montantAttendu apport fixé au contrat ; différent de {@code montant} en cas d'écart
 */
public record PaiementRapproche(UUID paiementId, UUID demandeId, UUID contratId, UUID clientId, Montant montant,
        Montant montantAttendu, LocalDate recuLe, Instant le) {

    public PaiementRapproche {
        Objects.requireNonNull(paiementId, "paiementId");
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(contratId, "contratId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(montant, "montant");
        Objects.requireNonNull(montantAttendu, "montantAttendu");
        Objects.requireNonNull(recuLe, "recuLe");
        Objects.requireNonNull(le, "le");
    }

    /** Le montant reçu est exactement l'apport fixé au contrat. */
    public boolean apportSolde() {
        return montant.compareTo(montantAttendu) == 0;
    }
}
