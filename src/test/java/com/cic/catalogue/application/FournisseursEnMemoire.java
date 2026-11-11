package com.cic.catalogue.application;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.catalogue.domain.Fournisseur;
import com.cic.catalogue.domain.FournisseurRepository;

/** Stockage en mémoire pour les tests ; stocke des copies pour imiter une base. */
public class FournisseursEnMemoire implements FournisseurRepository {

    private final Map<UUID, Fournisseur> parId = new LinkedHashMap<>();

    @Override
    public void enregistrer(Fournisseur f) {
        parId.put(f.id(), copie(f));
    }

    @Override
    public Optional<Fournisseur> parId(UUID id) {
        return Optional.ofNullable(parId.get(id)).map(FournisseursEnMemoire::copie);
    }

    @Override
    public Optional<Fournisseur> parNom(String nom) {
        String cherche = nom.toLowerCase(Locale.ROOT);
        return parId.values().stream()
                .filter(f -> f.nom().toLowerCase(Locale.ROOT).equals(cherche))
                .findFirst()
                .map(FournisseursEnMemoire::copie);
    }

    @Override
    public List<Fournisseur> tous() {
        return parId.values().stream()
                .sorted(Comparator.comparing(Fournisseur::nom))
                .map(FournisseursEnMemoire::copie)
                .toList();
    }

    private static Fournisseur copie(Fournisseur f) {
        return Fournisseur.reconstituer(f.id(), f.nom(), f.remiseNegocieePct(), f.estActif());
    }
}
