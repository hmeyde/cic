package com.cic.contrat.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContratJpaRepository extends JpaRepository<ContratEntity, UUID> {

    @EntityGraph(attributePaths = "echeances")
    Optional<ContratEntity> findByDemandeId(UUID demandeId);

    @EntityGraph(attributePaths = "echeances")
    Optional<ContratEntity> findWithEcheancesById(UUID id);
}
