package com.cic.identite.domain;

import java.util.Optional;
import java.util.UUID;

/** Stockage des codes de connexion : au plus un par client (port de sortie). */
public interface CodeConnexionRepository {

    /** Enregistre le code ; remplace le code précédent du même client. */
    void enregistrer(CodeConnexion code);

    Optional<CodeConnexion> parClient(UUID clientId);
}
