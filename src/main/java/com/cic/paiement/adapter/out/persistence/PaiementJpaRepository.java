package com.cic.paiement.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaiementJpaRepository extends JpaRepository<PaiementEntity, UUID> {

    Optional<PaiementEntity> findByReference(String reference);

    List<PaiementEntity> findAllByOrderBySaisiLeDesc(Pageable page);
}
