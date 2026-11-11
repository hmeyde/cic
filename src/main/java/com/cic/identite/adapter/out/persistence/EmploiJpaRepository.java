package com.cic.identite.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EmploiJpaRepository extends JpaRepository<EmploiEntity, UUID> {

    Optional<EmploiEntity> findByClientId(UUID clientId);
}
