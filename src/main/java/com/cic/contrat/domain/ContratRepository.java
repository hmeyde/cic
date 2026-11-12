package com.cic.contrat.domain;

import java.util.Optional;
import java.util.UUID;

/** Stockage des contrats et de leurs échéances (port de sortie). */
public interface ContratRepository {

    /** Insère un nouveau contrat avec ses échéances. */
    void enregistrer(Contrat contrat);

    /** Contrat d'un dossier ; au plus un par dossier. */
    Optional<Contrat> parDemande(UUID demandeId);

    /** Numéro d'ordre suivant, unique et croissant, pour numéroter les contrats. */
    long prochainNumeroOrdre();
}
