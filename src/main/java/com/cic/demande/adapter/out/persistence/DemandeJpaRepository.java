package com.cic.demande.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cic.demande.domain.StatutDossier;

interface DemandeJpaRepository extends JpaRepository<DemandeEntity, UUID> {

    List<DemandeEntity> findByClientIdOrderBySoumiseLeDesc(UUID clientId);

    /** S'appuie sur l'index {@code demande_statut_idx (statut, soumise_le)}. */
    List<DemandeEntity> findByStatutOrderBySoumiseLeAscIdAsc(StatutDossier statut);
}
