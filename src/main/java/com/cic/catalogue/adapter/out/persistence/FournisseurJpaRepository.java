package com.cic.catalogue.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface FournisseurJpaRepository extends JpaRepository<FournisseurEntity, UUID> {

    List<FournisseurEntity> findAllByOrderByNomAsc();
}
