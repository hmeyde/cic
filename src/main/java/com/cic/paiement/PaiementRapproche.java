package com.cic.paiement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Événement publié quand un paiement reçu est rattaché à un dossier par sa référence : référence
 * d'apport, par exemple pour faire passer le dossier à APPORT_ET_AUTORISATION quand l'apport est
 * encaissé et l'autorisation ACCEPTEE ; référence d'échéance, pour l'encaissement d'une mensualité.
 * Aucune donnée personnelle : seulement des identifiants et des montants.
 *
 * @param objet          apport ou échéance réglée par le paiement
 * @param rangEcheance   rang de l'échéance réglée ; {@code null} pour un apport
 * @param numeroContrat  numéro lisible du contrat, pour les libellés
 * @param montantAttendu apport ou échéance fixé au contrat ; différent de {@code montant} en cas d'écart
 */
public record PaiementRapproche(UUID paiementId, UUID demandeId, UUID contratId, String numeroContrat,
        UUID clientId, ObjetPaiement objet, Integer rangEcheance, Montant montant, Montant montantAttendu,
        LocalDate recuLe, Instant le) {

    public PaiementRapproche {
        Objects.requireNonNull(paiementId, "paiementId");
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(contratId, "contratId");
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(objet, "objet");
        Objects.requireNonNull(montant, "montant");
        Objects.requireNonNull(montantAttendu, "montantAttendu");
        Objects.requireNonNull(recuLe, "recuLe");
        Objects.requireNonNull(le, "le");
        if ((objet == ObjetPaiement.ECHEANCE) != (rangEcheance != null)) {
            throw new IllegalArgumentException("Rang d'échéance attendu pour une échéance, et seulement pour elle");
        }
    }

    public boolean estApport() {
        return objet == ObjetPaiement.APPORT;
    }

    /** Le montant reçu est exactement le montant fixé au contrat. */
    public boolean apportSolde() {
        return montant.compareTo(montantAttendu) == 0;
    }
}
