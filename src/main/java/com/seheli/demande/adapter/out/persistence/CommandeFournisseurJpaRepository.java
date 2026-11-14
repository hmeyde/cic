package com.seheli.demande.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommandeFournisseurJpaRepository extends JpaRepository<CommandeFournisseurEntity, UUID> {

    Optional<CommandeFournisseurEntity> findByDemandeId(UUID demandeId);

    List<CommandeFournisseurEntity> findAllByOrderByValideLeDesc(Pageable page);
}
