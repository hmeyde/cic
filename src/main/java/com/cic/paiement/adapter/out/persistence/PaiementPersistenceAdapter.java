package com.cic.paiement.adapter.out.persistence;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cic.commun.domain.Montant;
import com.cic.paiement.domain.ApportAttendu;
import com.cic.paiement.domain.Paiement;
import com.cic.paiement.domain.PaiementEnDoubleException;
import com.cic.paiement.domain.PaiementRepository;
import com.cic.paiement.domain.ReferencePaiement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

@Component
class PaiementPersistenceAdapter implements PaiementRepository {

    static final String CONTRAINTE_REFERENCE = "paiement_reference_uk";

    private final PaiementJpaRepository paiements;
    private final EntityManager entityManager;

    PaiementPersistenceAdapter(PaiementJpaRepository paiements, EntityManager entityManager) {
        this.paiements = paiements;
        this.entityManager = entityManager;
    }

    /** Insertion envoyée tout de suite à la base, pour que la contrainte d'unicité joue ici. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrer(Paiement p) {
        Optional<ApportAttendu> a = p.apportAttendu();
        try {
            entityManager.persist(new PaiementEntity(p.id(), p.reference().valeur(), p.montant().valeur(), p.recuLe(),
                    p.statut(), a.map(ApportAttendu::contratId).orElse(null),
                    a.map(ApportAttendu::numeroContrat).orElse(null), a.map(ApportAttendu::demandeId).orElse(null),
                    a.map(ApportAttendu::clientId).orElse(null), a.map(x -> x.montant().valeur()).orElse(null),
                    p.saisiPar(), p.saisiLe()));
            entityManager.flush();
        } catch (PersistenceException e) {
            if (violeUniciteReference(e)) {
                throw new PaiementEnDoubleException(null);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Paiement> parId(UUID id) {
        return paiements.findById(id).map(PaiementPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Paiement> parReference(ReferencePaiement reference) {
        return paiements.findByReference(reference.valeur()).map(PaiementPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Paiement> derniers(int limite) {
        return paiements.findAllByOrderBySaisiLeDesc(PageRequest.of(0, limite)).stream()
                .map(PaiementPersistenceAdapter::versDomaine).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Paiement> ecarts(int limite) {
        return paiements.findEcarts(PageRequest.of(0, limite)).stream().map(PaiementPersistenceAdapter::versDomaine)
                .toList();
    }

    private static boolean violeUniciteReference(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException c && c.getConstraintName() != null
                    && c.getConstraintName().toLowerCase(Locale.ROOT).contains(CONTRAINTE_REFERENCE)) {
                return true;
            }
        }
        return false;
    }

    private static Paiement versDomaine(PaiementEntity e) {
        Optional<ApportAttendu> attendu = e.getContratId() == null ? Optional.empty()
                : Optional.of(new ApportAttendu(e.getContratId(), e.getNumeroContrat(), e.getDemandeId(),
                        e.getClientId(), Montant.de(e.getMontantAttendu())));
        return Paiement.reconstituer(e.getId(), new ReferencePaiement(e.getReference()), Montant.de(e.getMontant()),
                e.getRecuLe(), attendu, e.getSaisiPar(), e.getSaisiLe());
    }
}
