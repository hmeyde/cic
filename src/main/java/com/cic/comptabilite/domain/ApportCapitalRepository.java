package com.cic.comptabilite.domain;

import java.util.List;

import com.cic.commun.domain.Montant;

/** Stockage des apports en capital (port de sortie) : insertion seulement. */
public interface ApportCapitalRepository {

    void enregistrer(ApportCapital apport);

    /** Tous les apports, du versement le plus récent au plus ancien. */
    List<ApportCapital> tous();

    /** Somme de tous les apports. */
    Montant total();
}
