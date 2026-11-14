package com.seheli.catalogue.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FournisseurJpaRepository extends JpaRepository<FournisseurEntity, UUID> {

    List<FournisseurEntity> findAllByOrderByNomAsc();

    /** Même règle que l'index unique {@code fournisseur_nom_uk} (lower(nom)). */
    @Query("select f from FournisseurEntity f where lower(f.nom) = lower(:nom)")
    Optional<FournisseurEntity> findByNomSansCasse(@Param("nom") String nom);
}
