package com.cic.contrat.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cic.contrat.domain.AutorisationPrelevement;
import com.cic.contrat.domain.AutorisationPrelevementRepository;

import jakarta.persistence.EntityManager;

@Component
class AutorisationPrelevementPersistenceAdapter implements AutorisationPrelevementRepository {

    private final AutorisationPrelevementJpaRepository autorisations;
    private final EntityManager entityManager;

    AutorisationPrelevementPersistenceAdapter(AutorisationPrelevementJpaRepository autorisations,
            EntityManager entityManager) {
        this.autorisations = autorisations;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void enregistrer(AutorisationPrelevement a) {
        entityManager.persist(new AutorisationPrelevementEntity(a.id(), a.contratId(), a.demandeId(), a.clientId(),
                a.type(), a.statut(), a.emiseLe()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AutorisationPrelevement> parContrat(UUID contratId) {
        return autorisations.findByContratId(contratId)
                .map(e -> AutorisationPrelevement.reconstituer(e.getId(), e.getContratId(), e.getDemandeId(),
                        e.getClientId(), e.getType(), e.getStatut(), e.getEmiseLe()));
    }
}
