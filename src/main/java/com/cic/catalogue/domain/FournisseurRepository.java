package com.cic.catalogue.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des fournisseurs (port de sortie, implémenté par l'adaptateur de persistance). */
public interface FournisseurRepository {

    void enregistrer(Fournisseur fournisseur);

    Optional<Fournisseur> parId(UUID id);

    /** Fournisseur portant ce nom, sans tenir compte de la casse. */
    Optional<Fournisseur> parNom(String nom);

    /** Tous les fournisseurs, triés par nom. */
    List<Fournisseur> tous();
}
