package com.cic.demande.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DemandeJpaRepository extends JpaRepository<DemandeEntity, UUID> {

    List<DemandeEntity> findByClientIdOrderBySoumiseLeDesc(UUID clientId);
}
