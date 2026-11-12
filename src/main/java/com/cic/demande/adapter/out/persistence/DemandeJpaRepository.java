package com.cic.demande.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.cic.demande.domain.StatutDossier;

interface DemandeJpaRepository extends JpaRepository<DemandeEntity, UUID> {

    List<DemandeEntity> findByClientIdOrderBySoumiseLeDesc(UUID clientId);

    /** S'appuie sur l'index {@code demande_statut_idx (statut, soumise_le)}. */
    List<DemandeEntity> findByStatutOrderBySoumiseLeAscIdAsc(StatutDossier statut);

    /** {@code SELECT … FOR UPDATE} : verrou sur la ligne jusqu'à la fin de la transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DemandeEntity d where d.id = :id")
    Optional<DemandeEntity> findVerrouilleeById(@Param("id") UUID id);
}
