package com.seheli.audit.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

/** Lecture seulement : l'ajout passe par {@code EntityManager.persist} ; ni mise à jour ni suppression. */
interface JournalAuditJpaRepository extends Repository<JournalAuditEntity, UUID> {

    /** S'appuie sur l'index {@code journal_audit_objet_idx}. */
    List<JournalAuditEntity> findByTypeObjetAndObjetIdOrderByLeAscNumeroAsc(String typeObjet, UUID objetId);
}
