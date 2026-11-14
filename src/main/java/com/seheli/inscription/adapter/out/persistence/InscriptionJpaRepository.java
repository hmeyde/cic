package com.seheli.inscription.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface InscriptionJpaRepository extends JpaRepository<InscriptionEntity, UUID> {

    boolean existsByTelephone(String telephone);

    /** S'appuie sur l'index {@code inscription_cree_le_idx}. */
    List<InscriptionEntity> findAllByOrderByCreeLeDescIdAsc();
}
