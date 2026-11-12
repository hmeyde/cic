package com.cic.recouvrement.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cic.recouvrement.domain.Relance;
import com.cic.recouvrement.domain.RelanceRepository;
import com.cic.recouvrement.domain.TypeRelance;

import jakarta.persistence.EntityManager;

@Component
class RelancePersistenceAdapter implements RelanceRepository {

    private final RelanceJpaRepository relances;
    private final EntityManager entityManager;

    RelancePersistenceAdapter(RelanceJpaRepository relances, EntityManager entityManager) {
        this.relances = relances;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existe(UUID echeanceId, TypeRelance type) {
        return relances.existsByEcheanceIdAndType(echeanceId, type);
    }

    /** Insertion immédiate : un doublon (contrainte relance_echeance_type_uk) échoue avant l'envoi du message. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrer(Relance r) {
        entityManager.persist(new RelanceEntity(r.id(), r.echeanceId(), r.contratId(), r.type(), r.dateEcheance(),
                r.envoyeeLe()));
        entityManager.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Relance> parContrat(UUID contratId) {
        return relances.findByContratIdOrderByEnvoyeeLeDesc(contratId).stream().map(RelanceEntity::versDomaine).toList();
    }
}
