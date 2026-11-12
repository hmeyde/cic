package com.cic.demande.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des demandes et de leur score (port de sortie, implémenté par l'adaptateur de persistance). */
public interface DemandeRepository {

    void enregistrer(Demande demande);

    Optional<Demande> parId(UUID id);

    /** Demandes du client, de la plus récente à la plus ancienne. */
    List<Demande> parClient(UUID clientId);

    /**
     * Sérialise les soumissions d'un même client jusqu'à la fin de la transaction en cours, pour que
     * deux demandes simultanées ne passent pas toutes deux la règle du dossier actif unique.
     */
    void verrouillerClient(UUID clientId);
}
