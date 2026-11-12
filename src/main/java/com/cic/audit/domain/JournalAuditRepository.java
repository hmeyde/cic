package com.cic.audit.domain;

import java.util.List;

/** Stockage du journal d'audit, en ajout seulement (port de sortie). */
public interface JournalAuditRepository {

    void ajouter(EntreeJournal entree);

    /** Entrées sur l'objet, de la plus ancienne à la plus récente (ordre d'écriture à instant égal). */
    List<EntreeJournal> parObjet(ObjetAudite objet);
}
