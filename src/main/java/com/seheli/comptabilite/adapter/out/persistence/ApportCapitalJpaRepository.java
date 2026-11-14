package com.seheli.comptabilite.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ApportCapitalJpaRepository extends JpaRepository<ApportCapitalEntity, UUID> {

    List<ApportCapitalEntity> findAllByOrderByVerseLeDescSaisiLeDesc();

    @Query("select coalesce(sum(a.montant), 0) from ApportCapitalEntity a")
    BigDecimal total();
}
