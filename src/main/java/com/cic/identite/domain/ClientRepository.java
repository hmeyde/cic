package com.cic.identite.domain;

import java.util.Optional;
import java.util.UUID;

/** Stockage des clients (port de sortie, implémenté par l'adaptateur de persistance). */
public interface ClientRepository {

    void enregistrer(Client client);

    Optional<Client> parId(UUID id);

    /** Client portant ce NNI (forme normalisée, voir {@link Client#normaliserNni}). */
    Optional<Client> parNni(String nni);

    /** Client portant ce téléphone (forme normalisée, voir {@link Client#normaliserTelephone}). */
    Optional<Client> parTelephone(String telephone);
}
