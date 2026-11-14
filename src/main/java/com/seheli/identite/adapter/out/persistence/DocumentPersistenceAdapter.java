package com.seheli.identite.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.identite.domain.Document;
import com.seheli.identite.domain.DocumentRepository;
import com.seheli.identite.domain.TypeDocument;

@Component
class DocumentPersistenceAdapter implements DocumentRepository {

    private final DocumentJpaRepository repository;

    DocumentPersistenceAdapter(DocumentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void enregistrer(Document d) {
        repository.save(new DocumentEntity(d.id(), d.clientId(), d.type(), d.format(), d.tailleOctets(), d.recuLe()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> parId(UUID id) {
        return repository.findById(id).map(DocumentPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> parClient(UUID clientId) {
        return repository.findByClientIdOrderByRecuLeDesc(clientId).stream()
                .map(DocumentPersistenceAdapter::versDomaine).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long compterParClientEtType(UUID clientId, TypeDocument type) {
        return repository.countByClientIdAndType(clientId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> derniers(int limite) {
        return repository.findAllByOrderByRecuLeDesc(Limit.of(limite)).stream()
                .map(DocumentPersistenceAdapter::versDomaine).toList();
    }

    private static Document versDomaine(DocumentEntity e) {
        return Document.reconstituer(e.getId(), e.getClientId(), e.getType(), e.getFormat(), e.getTailleOctets(),
                e.getRecuLe());
    }
}
