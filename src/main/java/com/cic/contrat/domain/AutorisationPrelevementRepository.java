package com.cic.contrat.domain;

import java.util.Optional;
import java.util.UUID;

/** Stockage des autorisations de prélèvement (port de sortie). */
public interface AutorisationPrelevementRepository {

    /** Insère une nouvelle autorisation. */
    void enregistrer(AutorisationPrelevement autorisation);

    /** Autorisation du contrat ; au plus une par contrat. */
    Optional<AutorisationPrelevement> parContrat(UUID contratId);
}
