package com.seheli.identite.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.identite.domain.CodeConnexion;
import com.seheli.identite.domain.CodeConnexionRepository;

@Component
class CodeConnexionPersistenceAdapter implements CodeConnexionRepository {

    private final CodeConnexionJpaRepository repository;

    CodeConnexionPersistenceAdapter(CodeConnexionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void enregistrer(CodeConnexion c) {
        // La clé est le téléphone : enregistrer un nouveau code remplace le précédent.
        repository.save(new CodeConnexionEntity(c.telephone(), c.codeHash(), c.emisLe(), c.expireLe(),
                (short) c.essaisMax(), (short) c.essais(), c.estUtilise()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CodeConnexion> parTelephone(String telephone) {
        return repository.findById(telephone)
                .map(e -> CodeConnexion.reconstituer(e.getTelephone(), e.getCodeHash(), e.getEmisLe(),
                        e.getExpireLe(), e.getEssaisMax(), e.getEssais(), e.isUtilise()));
    }
}
