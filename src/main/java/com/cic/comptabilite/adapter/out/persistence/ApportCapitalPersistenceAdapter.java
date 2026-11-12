package com.cic.comptabilite.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cic.commun.domain.Montant;
import com.cic.comptabilite.domain.ApportCapital;
import com.cic.comptabilite.domain.ApportCapitalRepository;

import jakarta.persistence.EntityManager;

@Component
class ApportCapitalPersistenceAdapter implements ApportCapitalRepository {

    private final ApportCapitalJpaRepository apports;
    private final EntityManager entityManager;

    ApportCapitalPersistenceAdapter(ApportCapitalJpaRepository apports, EntityManager entityManager) {
        this.apports = apports;
        this.entityManager = entityManager;
    }

    /** Insertion seulement : un apport n'est jamais mis à jour. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrer(ApportCapital apport) {
        entityManager.persist(new ApportCapitalEntity(apport));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApportCapital> tous() {
        return apports.findAllByOrderByVerseLeDescSaisiLeDesc().stream().map(ApportCapitalEntity::versDomaine)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Montant total() {
        return Montant.de(apports.total());
    }
}
