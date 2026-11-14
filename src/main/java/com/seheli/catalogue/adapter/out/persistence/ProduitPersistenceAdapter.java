package com.seheli.catalogue.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.catalogue.domain.Produit;
import com.seheli.catalogue.domain.ProduitRepository;
import com.seheli.commun.domain.Montant;

@Component
class ProduitPersistenceAdapter implements ProduitRepository {

    private final ProduitJpaRepository repository;

    ProduitPersistenceAdapter(ProduitJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void enregistrer(Produit p) {
        repository.save(new ProduitEntity(p.id(), p.reference(), p.designation(), p.marque(), p.categorie(),
                p.fournisseurId(), p.prixPublic().valeur(), p.estDisponible()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Produit> parId(UUID id) {
        return repository.findById(id).map(ProduitPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Produit> parReference(String reference) {
        return repository.findByReference(reference.strip()).map(ProduitPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produit> tous() {
        return versDomaine(repository.findAllByOrderByDesignationAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produit> disponibles() {
        return versDomaine(repository.findByDisponibleTrueOrderByDesignationAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produit> disponibles(CategorieProduit categorie) {
        return versDomaine(repository.findByDisponibleTrueAndCategorieOrderByDesignationAsc(categorie));
    }

    private static List<Produit> versDomaine(List<ProduitEntity> entites) {
        return entites.stream().map(ProduitPersistenceAdapter::versDomaine).toList();
    }

    private static Produit versDomaine(ProduitEntity e) {
        return Produit.reconstituer(e.getId(), e.getReference(), e.getDesignation(), e.getMarque(), e.getCategorie(),
                e.getFournisseurId(), Montant.de(e.getPrixPublic()), e.isDisponible());
    }
}
