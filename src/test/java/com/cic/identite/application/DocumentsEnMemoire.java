package com.cic.identite.application;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.identite.domain.Document;
import com.cic.identite.domain.DocumentRepository;
import com.cic.identite.domain.TypeDocument;

public class DocumentsEnMemoire implements DocumentRepository {

    private static final Comparator<Document> PLUS_RECENT_D_ABORD =
            Comparator.comparing(Document::recuLe).reversed();

    private final Map<UUID, Document> documents = new LinkedHashMap<>();

    @Override
    public void enregistrer(Document document) {
        documents.put(document.id(), document);
    }

    @Override
    public Optional<Document> parId(UUID id) {
        return Optional.ofNullable(documents.get(id));
    }

    @Override
    public List<Document> parClient(UUID clientId) {
        return documents.values().stream().filter(d -> d.appartientA(clientId)).sorted(PLUS_RECENT_D_ABORD).toList();
    }

    @Override
    public long compterParClientEtType(UUID clientId, TypeDocument type) {
        return documents.values().stream().filter(d -> d.appartientA(clientId) && d.type() == type).count();
    }

    @Override
    public List<Document> derniers(int limite) {
        return documents.values().stream().sorted(PLUS_RECENT_D_ABORD).limit(limite).toList();
    }
}
