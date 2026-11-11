package com.cic.identite.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CodeConnexionJpaRepository extends JpaRepository<CodeConnexionEntity, UUID> {
}
