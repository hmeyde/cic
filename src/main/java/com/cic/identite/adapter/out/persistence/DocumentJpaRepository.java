package com.cic.identite.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cic.identite.domain.TypeDocument;

interface DocumentJpaRepository extends JpaRepository<DocumentEntity, UUID> {

    List<DocumentEntity> findByClientIdOrderByRecuLeDesc(UUID clientId);

    long countByClientIdAndType(UUID clientId, TypeDocument type);

    List<DocumentEntity> findAllByOrderByRecuLeDesc(Limit limit);
}
