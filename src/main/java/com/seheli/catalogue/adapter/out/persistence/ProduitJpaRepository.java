package com.seheli.catalogue.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seheli.catalogue.domain.CategorieProduit;

interface ProduitJpaRepository extends JpaRepository<ProduitEntity, UUID> {

    Optional<ProduitEntity> findByReference(String reference);

    List<ProduitEntity> findAllByOrderByDesignationAsc();

    List<ProduitEntity> findByDisponibleTrueOrderByDesignationAsc();

    List<ProduitEntity> findByDisponibleTrueAndCategorieOrderByDesignationAsc(CategorieProduit categorie);
}
