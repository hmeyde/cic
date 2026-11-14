package com.cic.contrat.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des contrats et de leurs échéances (port de sortie). */
public interface ContratRepository {

    /** Insère un nouveau contrat avec ses échéances. */
    void enregistrer(Contrat contrat);

    /** Contrat avec ses échéances. */
    Optional<Contrat> parId(UUID id);

    /** Contrat d'un dossier ; au plus un par dossier. */
    Optional<Contrat> parDemande(UUID demandeId);

    /** Contrat portant ce numéro, par exemple {@code SEH-2026-000042} ; les numéros sont uniques. */
    Optional<Contrat> parNumero(String numero);

    /** Enregistre le nouveau statut d'une échéance du contrat ; seul le statut change après la signature. */
    void enregistrerStatut(UUID contratId, Echeance echeance);

    /** Échéances de ce statut dont la date est comprise entre {@code du} et {@code au} inclus, par date puis contrat. */
    List<EcheanceAPrelever> echeancesEntre(StatutEcheance statut, LocalDate du, LocalDate au);

    /** Échéances A_VENIR dont la date est comprise entre {@code du} et {@code au} inclus, par date puis contrat. */
    default List<EcheanceAPrelever> echeancesAVenirEntre(LocalDate du, LocalDate au) {
        return echeancesEntre(StatutEcheance.A_VENIR, du, au);
    }

    /** Le client a au moins une échéance EN_RETARD, sur l'un quelconque de ses contrats. */
    boolean clientAUneEcheanceEnRetard(UUID clientId);

    /** Numéro d'ordre suivant, unique et croissant, pour numéroter les contrats. */
    long prochainNumeroOrdre();
}
