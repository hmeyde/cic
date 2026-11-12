package com.cic.contrat.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AutorisationPrelevementJpaRepository extends JpaRepository<AutorisationPrelevementEntity, UUID> {

    Optional<AutorisationPrelevementEntity> findByContratId(UUID contratId);
}
