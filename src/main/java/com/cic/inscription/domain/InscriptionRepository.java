package com.cic.inscription.domain;

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
}
