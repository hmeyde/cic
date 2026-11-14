package com.seheli.identite.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.identite.domain.UtilisateurBackOffice;
import com.seheli.identite.domain.UtilisateurBackOfficeRepository;

@Component
class UtilisateurBackOfficePersistenceAdapter implements UtilisateurBackOfficeRepository {

    private final UtilisateurBackOfficeJpaRepository repository;

    UtilisateurBackOfficePersistenceAdapter(UtilisateurBackOfficeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void enregistrer(UtilisateurBackOffice u) {
        repository.save(new UtilisateurBackOfficeEntity(u.id(), u.identifiant(), u.motDePasseHash(), u.role(),
                u.estActif()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UtilisateurBackOffice> parIdentifiant(String identifiantNormalise) {
        return repository.findByIdentifiantIgnoreCase(identifiantNormalise)
                .map(e -> UtilisateurBackOffice.reconstituer(e.getId(), e.getIdentifiant(), e.getMotDePasseHash(),
                        e.getRole(), e.isActif()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existe(String identifiantNormalise) {
        return repository.existsByIdentifiantIgnoreCase(identifiantNormalise);
    }
}
