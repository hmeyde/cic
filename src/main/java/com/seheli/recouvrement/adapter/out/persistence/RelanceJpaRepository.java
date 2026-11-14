package com.seheli.recouvrement.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seheli.recouvrement.domain.TypeRelance;

interface RelanceJpaRepository extends JpaRepository<RelanceEntity, UUID> {

    boolean existsByEcheanceIdAndType(UUID echeanceId, TypeRelance type);

    List<RelanceEntity> findByContratIdOrderByEnvoyeeLeDesc(UUID contratId);
}
