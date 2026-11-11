package com.cic.identite.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cic.commun.domain.Montant;
import com.cic.identite.domain.Emploi;
import com.cic.identite.domain.EmploiRepository;

@Component
class EmploiPersistenceAdapter implements EmploiRepository {

    private final EmploiJpaRepository repository;

    EmploiPersistenceAdapter(EmploiJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void enregistrer(Emploi e) {
        repository.save(new EmploiEntity(e.id(), e.clientId(), e.employeur(), e.typeContrat(), e.dateDebut(),
                e.dateFin().orElse(null), e.salaireNet().valeur(), (short) e.jourPaie(),
                e.estSalaireVerseSurCompte(), e.estSalaireDomicilieBmci()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Emploi> parClient(UUID clientId) {
        return repository.findByClientId(clientId).map(EmploiPersistenceAdapter::versDomaine);
    }

    private static Emploi versDomaine(EmploiEntity e) {
        return Emploi.reconstituer(e.getId(), e.getClientId(), e.getEmployeur(), e.getTypeContrat(),
                e.getDateDebut(), e.getDateFin(), Montant.de(e.getSalaireNet()), e.getJourPaie(),
                e.isSalaireVerseSurCompte(), e.isSalaireDomicilieBmci());
    }
}
