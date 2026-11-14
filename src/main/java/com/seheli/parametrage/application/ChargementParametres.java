package com.seheli.parametrage.application;

import java.util.List;

import com.seheli.parametrage.domain.Parametre;

/** Port de sortie : lecture de toutes les lignes de la table {@code parametre}. */
public interface ChargementParametres {

    /** @throws com.seheli.parametrage.domain.ParametreInvalideException si une ligne est invalide */
    List<Parametre> chargerTous();
}
