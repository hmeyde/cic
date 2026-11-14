package com.seheli.catalogue.application;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.catalogue.domain.Produit;
import com.seheli.catalogue.domain.ProduitRepository;

/** Stockage en mémoire pour les tests ; stocke des copies pour imiter une base. */
public class ProduitsEnMemoire implements ProduitRepository {

    private final Map<UUID, Produit> parId = new LinkedHashMap<>();

    @Override
    public void enregistrer(Produit p) {
        parId.put(p.id(), copie(p));
    }

    @Override
    public Optional<Produit> parId(UUID id) {
        return Optional.ofNullable(parId.get(id)).map(ProduitsEnMemoire::copie);
    }

    @Override
    public Optional<Produit> parReference(String reference) {
        String cherche = reference.strip();
        return parId.values().stream().filter(p -> p.reference().equals(cherche)).findFirst()
                .map(ProduitsEnMemoire::copie);
    }

    @Override
    public List<Produit> tous() {
        return parId.values().stream().sorted(Comparator.comparing(Produit::designation))
                .map(ProduitsEnMemoire::copie).toList();
    }

    @Override
    public List<Produit> disponibles() {
        return tous().stream().filter(Produit::estDisponible).toList();
    }

    @Override
    public List<Produit> disponibles(CategorieProduit categorie) {
        return disponibles().stream().filter(p -> p.categorie() == categorie).toList();
    }

    private static Produit copie(Produit p) {
        return Produit.reconstituer(p.id(), p.reference(), p.designation(), p.marque(), p.categorie(),
                p.fournisseurId(), p.prixPublic(), p.estDisponible());
    }
}
