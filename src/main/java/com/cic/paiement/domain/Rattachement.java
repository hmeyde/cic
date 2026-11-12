package com.cic.paiement.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.Montant;
import com.cic.paiement.ObjetPaiement;

/**
 * Dossier auquel renvoie la référence d'un paiement : son contrat, le dossier et le client
 * (identifiants d'autres modules), ce que le paiement règle (l'apport ou une échéance, avec son rang)
 * et le montant fixé au contrat pour cet objet.
 *
 * @param rangEcheance rang de l'échéance réglée ; {@code null} pour l'apport
 */
public record Rattachement(UUID contratId, String numeroContrat, UUID demandeId, UUID clientId, ObjetPaiement objet,
        Integer rangEcheance, Montant montant) {

    public Rattachement {
        Objects.requireNonNull(contratId, "contratId");
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(objet, "objet");
        Objects.requireNonNull(montant, "montant");
        if (objet == ObjetPaiement.APPORT && rangEcheance != null) {
            throw new IllegalArgumentException("Un apport n'a pas de rang d'échéance");
        }
        if (objet == ObjetPaiement.ECHEANCE && (rangEcheance == null || rangEcheance <= 0)) {
            throw new IllegalArgumentException("Rang d'échéance invalide : " + rangEcheance);
        }
    }

    /** Paiement de l'apport du contrat. */
    public static Rattachement apport(UUID contratId, String numeroContrat, UUID demandeId, UUID clientId,
            Montant apport) {
        return new Rattachement(contratId, numeroContrat, demandeId, clientId, ObjetPaiement.APPORT, null, apport);
    }

    /** Paiement de l'échéance de ce rang. */
    public static Rattachement echeance(UUID contratId, String numeroContrat, UUID demandeId, UUID clientId,
            int rang, Montant montantEcheance) {
        return new Rattachement(contratId, numeroContrat, demandeId, clientId, ObjetPaiement.ECHEANCE, rang,
                montantEcheance);
    }

    public boolean estApport() {
        return objet == ObjetPaiement.APPORT;
    }

    public Optional<Integer> rang() {
        return Optional.ofNullable(rangEcheance);
    }

    /** Libellé court, sans donnée personnelle : « apport du contrat … » ou « échéance n du contrat … ». */
    public String libelle() {
        return estApport() ? "Apport du contrat " + numeroContrat
                : "Échéance " + rangEcheance + " du contrat " + numeroContrat;
    }
}
