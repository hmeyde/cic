package com.cic.identite.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cic.identite.domain.CodeConnexion;
import com.cic.identite.domain.CodeConnexionRepository;

@Component
class CodeConnexionPersistenceAdapter implements CodeConnexionRepository {

    private final CodeConnexionJpaRepository repository;

    CodeConnexionPersistenceAdapter(CodeConnexionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void enregistrer(CodeConnexion c) {
        // La clé est le client : enregistrer un nouveau code remplace le précédent.
        repository.save(new CodeConnexionEntity(c.clientId(), c.codeHash(), c.emisLe(), c.expireLe(),
                (short) c.essaisMax(), (short) c.essais(), c.estUtilise()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CodeConnexion> parClient(UUID clientId) {
        return repository.findById(clientId)
                .map(e -> CodeConnexion.reconstituer(e.getClientId(), e.getCodeHash(), e.getEmisLe(),
                        e.getExpireLe(), e.getEssaisMax(), e.getEssais(), e.isUtilise()));
    }
}
