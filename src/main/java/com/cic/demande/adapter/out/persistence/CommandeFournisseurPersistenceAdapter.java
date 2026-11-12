package com.cic.demande.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cic.commun.domain.Montant;
import com.cic.demande.domain.CommandeFournisseur;
import com.cic.demande.domain.CommandeFournisseurRepository;

import jakarta.persistence.EntityManager;

@Component
class CommandeFournisseurPersistenceAdapter implements CommandeFournisseurRepository {

    private final CommandeFournisseurJpaRepository commandes;
    private final EntityManager entityManager;

    CommandeFournisseurPersistenceAdapter(CommandeFournisseurJpaRepository commandes, EntityManager entityManager) {
        this.commandes = commandes;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrer(CommandeFournisseur c) {
        entityManager.persist(new CommandeFournisseurEntity(c.id(), c.numero(), c.demandeId(), c.contratId(),
                c.numeroContrat(), c.fournisseurId(), c.produitId(), c.designationProduit(), c.montant().valeur(),
                c.validePar(), c.valideLe()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommandeFournisseur> parDemande(UUID demandeId) {
        return commandes.findByDemandeId(demandeId).map(CommandeFournisseurPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommandeFournisseur> dernieres(int limite) {
        return commandes.findAllByOrderByValideLeDesc(PageRequest.of(0, limite)).stream()
                .map(CommandeFournisseurPersistenceAdapter::versDomaine).toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long prochainNumeroOrdre() {
        return ((Number) entityManager.createNativeQuery("SELECT nextval('commande_fournisseur_numero_seq')")
                .getSingleResult()).longValue();
    }

    private static CommandeFournisseur versDomaine(CommandeFournisseurEntity e) {
        return new CommandeFournisseur(e.getId(), e.getNumero(), e.getDemandeId(), e.getContratId(),
                e.getNumeroContrat(), e.getFournisseurId(), e.getProduitId(), e.getDesignationProduit(),
                Montant.de(e.getMontant()), e.getValidePar(), e.getValideLe());
    }
}
