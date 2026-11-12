package com.cic.recouvrement.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cic.recouvrement.domain.TypeRelance;

interface RelanceJpaRepository extends JpaRepository<RelanceEntity, UUID> {

    boolean existsByEcheanceIdAndType(UUID echeanceId, TypeRelance type);
}
