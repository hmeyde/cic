package com.seheli.comptabilite.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.seheli.comptabilite.domain.NatureEcriture;

interface EcritureComptableJpaRepository extends JpaRepository<EcritureComptableEntity, UUID> {

    Optional<EcritureComptableEntity> findByNatureAndSourceId(NatureEcriture nature, UUID sourceId);

    List<EcritureComptableEntity> findByDemandeIdOrderByPasseeLeAscIdAsc(UUID demandeId);

    List<EcritureComptableEntity> findAllByOrderByDateEcritureDescPasseeLeDesc(Pageable page);
}
