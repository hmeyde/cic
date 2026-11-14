package com.seheli.identite.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.seheli.identite.domain.UtilisateurBackOffice;
import com.seheli.identite.domain.UtilisateurBackOfficeRepository;

/** Stockage en mémoire pour les tests. */
public class UtilisateursEnMemoire implements UtilisateurBackOfficeRepository {

    private final Map<String, UtilisateurBackOffice> parIdentifiant = new HashMap<>();

    @Override
    public void enregistrer(UtilisateurBackOffice u) {
        parIdentifiant.put(u.identifiant(), u);
    }

    @Override
    public Optional<UtilisateurBackOffice> parIdentifiant(String identifiantNormalise) {
        return Optional.ofNullable(parIdentifiant.get(identifiantNormalise));
    }

    @Override
    public boolean existe(String identifiantNormalise) {
        return parIdentifiant.containsKey(identifiantNormalise);
    }

    public int nombre() {
        return parIdentifiant.size();
    }
}
