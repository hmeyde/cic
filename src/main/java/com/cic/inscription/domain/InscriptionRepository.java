package com.cic.inscription.domain;

import java.util.List;

/** Stockage des pré-inscriptions (port de sortie). */
public interface InscriptionRepository {

    /**
     * Insère une pré-inscription.
     *
     * @throws TelephoneDejaInscritException le téléphone est déjà inscrit (contrainte d'unicité, y compris
     *                                       en cas d'envois simultanés)
     */
    void enregistrer(Inscription inscription);

    /** @param telephone téléphone normalisé (8 chiffres) */
    boolean existeTelephone(String telephone);

    /** Nombre total de pré-inscriptions. */
    long nombre();

    /** Toutes les pré-inscriptions, de la plus récente à la plus ancienne. */
    List<Inscription> toutesDeLaPlusRecente();
}
