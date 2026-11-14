package com.seheli.identite.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UtilisateurBackOfficeJpaRepository extends JpaRepository<UtilisateurBackOfficeEntity, UUID> {

    Optional<UtilisateurBackOfficeEntity> findByIdentifiantIgnoreCase(String identifiant);

    boolean existsByIdentifiantIgnoreCase(String identifiant);
}
