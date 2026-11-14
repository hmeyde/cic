package com.seheli.contrat.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.seheli.contrat.domain.StatutAutorisation;

import jakarta.persistence.LockModeType;

interface AutorisationPrelevementJpaRepository extends JpaRepository<AutorisationPrelevementEntity, UUID> {

    Optional<AutorisationPrelevementEntity> findByContratId(UUID contratId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AutorisationPrelevementEntity a where a.id = :id")
    Optional<AutorisationPrelevementEntity> findVerrouilleeById(@Param("id") UUID id);

    List<AutorisationPrelevementEntity> findAllByOrderByEmiseLeDesc(Pageable page);

    List<AutorisationPrelevementEntity> findByStatutOrderByEmiseLeDesc(StatutAutorisation statut, Pageable page);
}
