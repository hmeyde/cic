package com.cic.contrat.domain;

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

    /** Contrat portant ce numéro, par exemple {@code CIC-2026-000042} ; les numéros sont uniques. */
    Optional<Contrat> parNumero(String numero);

    /** Enregistre le nouveau statut d'une échéance du contrat ; seul le statut change après la signature. */
    void enregistrerStatut(UUID contratId, Echeance echeance);

    /** Numéro d'ordre suivant, unique et croissant, pour numéroter les contrats. */
    long prochainNumeroOrdre();
}
