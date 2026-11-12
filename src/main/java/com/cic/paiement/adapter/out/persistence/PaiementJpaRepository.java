package com.cic.paiement.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaiementJpaRepository extends JpaRepository<PaiementEntity, UUID> {

    Optional<PaiementEntity> findByReference(String reference);

    List<PaiementEntity> findAllByOrderBySaisiLeDesc(Pageable page);

    @Query("""
            select p from PaiementEntity p
            where p.contratId is null or p.montant <> p.montantAttendu
            order by p.recuLe desc, p.saisiLe desc
            """)
    List<PaiementEntity> findEcarts(Pageable page);

    /** Somme des paiements rattachés au dossier ; zéro s'il n'en a aucun. */
    @Query("select coalesce(sum(p.montant), 0) from PaiementEntity p where p.demandeId = :demandeId")
    BigDecimal sommeParDemande(@Param("demandeId") UUID demandeId);
}
