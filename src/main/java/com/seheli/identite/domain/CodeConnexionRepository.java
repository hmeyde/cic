package com.seheli.identite.domain;

import java.util.Optional;

/** Stockage des codes de connexion : au plus un par téléphone (port de sortie). */
public interface CodeConnexionRepository {

    /** Enregistre le code ; remplace le code précédent du même téléphone. */
    void enregistrer(CodeConnexion code);

    /** Code en cours pour ce téléphone (forme normalisée, voir {@link Client#normaliserTelephone}). */
    Optional<CodeConnexion> parTelephone(String telephone);
}
