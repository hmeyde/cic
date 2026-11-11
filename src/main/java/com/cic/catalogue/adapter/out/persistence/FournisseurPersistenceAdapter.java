package com.cic.catalogue.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cic.catalogue.domain.Fournisseur;
import com.cic.catalogue.domain.FournisseurRepository;

@Component
class FournisseurPersistenceAdapter implements FournisseurRepository {

    private final FournisseurJpaRepository repository;

    FournisseurPersistenceAdapter(FournisseurJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void enregistrer(Fournisseur f) {
        repository.save(new FournisseurEntity(f.id(), f.nom(), f.remiseNegocieePct(), f.estActif()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Fournisseur> parId(UUID id) {
        return repository.findById(id).map(FournisseurPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Fournisseur> parNom(String nom) {
        return repository.findByNomSansCasse(nom).map(FournisseurPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fournisseur> tous() {
        return repository.findAllByOrderByNomAsc().stream().map(FournisseurPersistenceAdapter::versDomaine).toList();
    }

    private static Fournisseur versDomaine(FournisseurEntity e) {
        return Fournisseur.reconstituer(e.getId(), e.getNom(), e.getRemiseNegocieePct(), e.isActif());
    }
}
