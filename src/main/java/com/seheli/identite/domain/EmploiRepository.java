package com.seheli.identite.domain;

import java.util.Optional;
import java.util.UUID;

/** Stockage des emplois (port de sortie, implémenté par l'adaptateur de persistance). */
public interface EmploiRepository {

    void enregistrer(Emploi emploi);

    /** Emploi actuel du client. */
    Optional<Emploi> parClient(UUID clientId);
}
