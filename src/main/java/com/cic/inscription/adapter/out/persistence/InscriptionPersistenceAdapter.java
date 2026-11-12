package com.cic.inscription.adapter.out.persistence;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cic.inscription.domain.Inscription;
import com.cic.inscription.domain.InscriptionRepository;
import com.cic.inscription.domain.TelephoneDejaInscritException;

@Component
class InscriptionPersistenceAdapter implements InscriptionRepository {

    private final InscriptionJpaRepository inscriptions;

    InscriptionPersistenceAdapter(InscriptionJpaRepository inscriptions) {
        this.inscriptions = inscriptions;
    }

    /**
     * Insertion immédiate ({@code saveAndFlush}) : la contrainte d'unicité du téléphone tranche entre deux
     * envois simultanés du même numéro, et l'échec devient l'erreur métier du doublon.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrer(Inscription inscription) {
        try {
            inscriptions.saveAndFlush(new InscriptionEntity(inscription));
        } catch (DataIntegrityViolationException e) {
            throw new TelephoneDejaInscritException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeTelephone(String telephone) {
        return inscriptions.existsByTelephone(telephone);
    }

    @Override
    @Transactional(readOnly = true)
    public long nombre() {
        return inscriptions.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inscription> toutesDeLaPlusRecente() {
        return inscriptions.findAllByOrderByCreeLeDescIdAsc().stream().map(InscriptionEntity::versDomaine).toList();
    }
}
