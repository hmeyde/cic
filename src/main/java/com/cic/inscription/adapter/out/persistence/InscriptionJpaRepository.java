package com.cic.inscription.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface InscriptionJpaRepository extends JpaRepository<InscriptionEntity, UUID> {

    boolean existsByTelephone(String telephone);
}
