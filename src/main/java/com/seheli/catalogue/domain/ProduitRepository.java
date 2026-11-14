package com.seheli.catalogue.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des produits (port de sortie, implémenté par l'adaptateur de persistance). */
public interface ProduitRepository {

    void enregistrer(Produit produit);

    Optional<Produit> parId(UUID id);

    Optional<Produit> parReference(String reference);

    /** Tous les produits, triés par désignation. */
    List<Produit> tous();

    /** Produits proposés aux clients, triés par désignation. */
    List<Produit> disponibles();

    /** Produits proposés aux clients dans une catégorie, triés par désignation. */
    List<Produit> disponibles(CategorieProduit categorie);
}
