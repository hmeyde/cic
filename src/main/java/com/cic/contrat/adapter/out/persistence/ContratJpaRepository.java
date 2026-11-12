package com.cic.contrat.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cic.contrat.domain.StatutEcheance;

interface ContratJpaRepository extends JpaRepository<ContratEntity, UUID> {

    @EntityGraph(attributePaths = "echeances")
    Optional<ContratEntity> findByDemandeId(UUID demandeId);

    @EntityGraph(attributePaths = "echeances")
    Optional<ContratEntity> findByNumero(String numero);

    @EntityGraph(attributePaths = "echeances")
    Optional<ContratEntity> findWithEcheancesById(UUID id);

    /** Contrats ayant au moins une échéance de ce statut entre ces dates incluses (index echeance_statut_date_idx). */
    @EntityGraph(attributePaths = "echeances")
    @Query("""
            select c from ContratEntity c
            where exists (select 1 from EcheanceEntity e where e.contrat = c and e.statut = :statut
                          and e.dateEcheance between :du and :au)
            """)
    List<ContratEntity> findAvecEcheanceEntre(StatutEcheance statut, LocalDate du, LocalDate au);

    /** Le client a au moins une échéance de ce statut, sur l'un quelconque de ses contrats. */
    @Query("""
            select count(e) > 0 from EcheanceEntity e
            where e.contrat.clientId = :clientId and e.statut = :statut
            """)
    boolean existeEcheanceDuClient(UUID clientId, StatutEcheance statut);
}
