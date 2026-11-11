package com.cic.parametrage.application;

import java.util.List;

import com.cic.parametrage.domain.Parametre;

/** Port de sortie : lecture de toutes les lignes de la table {@code parametre}. */
public interface ChargementParametres {

    /** @throws com.cic.parametrage.domain.ParametreInvalideException si une ligne est invalide */
    List<Parametre> chargerTous();
}
