package com.seheli.identite.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Métadonnées des justificatifs (port de sortie, implémenté par l'adaptateur de persistance). */
public interface DocumentRepository {

    void enregistrer(Document document);

    Optional<Document> parId(UUID id);

    /** Documents du client, du plus récent au plus ancien. */
    List<Document> parClient(UUID clientId);

    long compterParClientEtType(UUID clientId, TypeDocument type);

    /** Derniers documents reçus, tous clients confondus, du plus récent au plus ancien. */
    List<Document> derniers(int limite);
}
