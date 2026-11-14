package com.seheli.demande.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.seheli.demande.domain.StatutDossier;

interface DemandeJpaRepository extends JpaRepository<DemandeEntity, UUID> {

    List<DemandeEntity> findByClientIdOrderBySoumiseLeDesc(UUID clientId);

    /** S'appuie sur l'index {@code demande_statut_idx (statut, soumise_le)}. */
    List<DemandeEntity> findByStatutOrderBySoumiseLeAscIdAsc(StatutDossier statut);

    /** S'appuie sur l'index {@code demande_statut_change_idx (statut, statut_change_le)}. */
    @Query("select d.id from DemandeEntity d where d.statut = :statut and d.statutChangeLe <= :limite"
            + " order by d.statutChangeLe, d.id")
    List<UUID> findIdsByStatutAndStatutChangeLeAuPlusTard(@Param("statut") StatutDossier statut,
            @Param("limite") Instant limite);

    /** {@code SELECT … FOR UPDATE} : verrou sur la ligne jusqu'à la fin de la transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DemandeEntity d where d.id = :id")
    Optional<DemandeEntity> findVerrouilleeById(@Param("id") UUID id);
}
