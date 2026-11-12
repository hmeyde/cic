package com.cic.recouvrement.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cic.recouvrement.domain.NoteRecouvrement;
import com.cic.recouvrement.domain.NoteRecouvrementRepository;

import jakarta.persistence.EntityManager;

@Component
class NoteRecouvrementPersistenceAdapter implements NoteRecouvrementRepository {

    private final NoteRecouvrementJpaRepository notes;
    private final EntityManager entityManager;

    NoteRecouvrementPersistenceAdapter(NoteRecouvrementJpaRepository notes, EntityManager entityManager) {
        this.notes = notes;
        this.entityManager = entityManager;
    }

    /** Insertion seulement : une note n'est jamais mise à jour. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrer(NoteRecouvrement note) {
        entityManager.persist(new NoteRecouvrementEntity(note));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteRecouvrement> parDemande(UUID demandeId) {
        return notes.findByDemandeIdOrderByEcriteLeDescIdDesc(demandeId).stream()
                .map(NoteRecouvrementEntity::versDomaine).toList();
    }
}
