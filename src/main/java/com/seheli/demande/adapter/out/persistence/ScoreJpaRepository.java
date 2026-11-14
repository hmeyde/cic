package com.seheli.demande.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ScoreJpaRepository extends JpaRepository<ScoreEntity, UUID> {

    List<ScoreEntity> findByDemandeIdIn(List<UUID> demandeIds);
}
