package com.seheli.audit.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.audit.domain.Acteur;
import com.seheli.audit.domain.EntreeJournal;
import com.seheli.audit.domain.JournalAuditRepository;
import com.seheli.audit.domain.ObjetAudite;

import jakarta.persistence.EntityManager;

@Component
class JournalAuditPersistenceAdapter implements JournalAuditRepository {

    private final JournalAuditJpaRepository journal;
    private final EntityManager entityManager;

    JournalAuditPersistenceAdapter(JournalAuditJpaRepository journal, EntityManager entityManager) {
        this.journal = journal;
        this.entityManager = entityManager;
    }

    /** {@code persist} plutôt que {@code save} : une entrée est toujours nouvelle, jamais fusionnée. */
    @Override
    @Transactional
    public void ajouter(EntreeJournal e) {
        entityManager.persist(new JournalAuditEntity(e.id(), e.le(), e.acteur().type(), e.acteur().identifiant(),
                e.action(), e.objet().type(), e.objet().id(), e.avant().orElse(null), e.apres().orElse(null),
                e.detail().orElse(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntreeJournal> parObjet(ObjetAudite objet) {
        return journal.findByTypeObjetAndObjetIdOrderByLeAscNumeroAsc(objet.type(), objet.id()).stream()
                .map(JournalAuditPersistenceAdapter::versDomaine).toList();
    }

    private static EntreeJournal versDomaine(JournalAuditEntity e) {
        return new EntreeJournal(e.getId(), e.getLe(), new Acteur(e.getTypeActeur(), e.getActeur()), e.getAction(),
                new ObjetAudite(e.getTypeObjet(), e.getObjetId()), Optional.ofNullable(e.getAvant()),
                Optional.ofNullable(e.getApres()), Optional.ofNullable(e.getDetail()));
    }
}
