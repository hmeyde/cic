package com.seheli.identite.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClientJpaRepository extends JpaRepository<ClientEntity, UUID> {

    Optional<ClientEntity> findByNni(String nni);

    Optional<ClientEntity> findByTelephone(String telephone);
}
