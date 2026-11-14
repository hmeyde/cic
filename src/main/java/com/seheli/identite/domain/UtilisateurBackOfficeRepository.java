package com.seheli.identite.domain;

import java.util.Optional;

/** Stockage des comptes du back-office (port de sortie, implémenté par l'adaptateur de persistance). */
public interface UtilisateurBackOfficeRepository {

    void enregistrer(UtilisateurBackOffice utilisateur);

    /** Recherche insensible à la casse ; l'identifiant reçu doit être déjà normalisé. */
    Optional<UtilisateurBackOffice> parIdentifiant(String identifiantNormalise);

    boolean existe(String identifiantNormalise);
}
