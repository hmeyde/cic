package com.seheli.contrat.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.contrat.domain.AutorisationPrelevement;
import com.seheli.contrat.domain.AutorisationPrelevementRepository;
import com.seheli.contrat.domain.DocumentSigne;
import com.seheli.contrat.domain.StatutAutorisation;

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
        AutorisationPrelevementEntity entite = new AutorisationPrelevementEntity(a.id(), a.contratId(), a.demandeId(),
                a.clientId(), a.type(), a.emiseLe());
        copierSuivi(a, entite);
        entityManager.persist(entite);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void mettreAJour(AutorisationPrelevement a) {
        AutorisationPrelevementEntity entite = autorisations.findById(a.id())
                .orElseThrow(() -> new IllegalStateException("Autorisation introuvable : " + a));
        copierSuivi(a, entite);
    }

    private static void copierSuivi(AutorisationPrelevement a, AutorisationPrelevementEntity entite) {
        Optional<DocumentSigne> d = a.documentSigne();
        entite.suivi(a.statut(), a.statutChangeLe(), d.map(DocumentSigne::id).orElse(null),
                d.map(DocumentSigne::format).orElse(null), d.map(DocumentSigne::tailleOctets).orElse(null),
                d.map(DocumentSigne::deposeLe).orElse(null), d.map(DocumentSigne::deposePar).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AutorisationPrelevement> parId(UUID id) {
        return autorisations.findById(id).map(AutorisationPrelevementPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<AutorisationPrelevement> parIdVerrouillee(UUID id) {
        return autorisations.findVerrouilleeById(id).map(AutorisationPrelevementPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AutorisationPrelevement> parContrat(UUID contratId) {
        return autorisations.findByContratId(contratId).map(AutorisationPrelevementPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutorisationPrelevement> dernieres(Optional<StatutAutorisation> statut, int limite) {
        PageRequest page = PageRequest.of(0, limite);
        return statut.map(s -> autorisations.findByStatutOrderByEmiseLeDesc(s, page))
                .orElseGet(() -> autorisations.findAllByOrderByEmiseLeDesc(page))
                .stream().map(AutorisationPrelevementPersistenceAdapter::versDomaine).toList();
    }

    private static AutorisationPrelevement versDomaine(AutorisationPrelevementEntity e) {
        DocumentSigne document = e.getDocumentSigneId() == null ? null
                : new DocumentSigne(e.getDocumentSigneId(), e.getDocumentSigneFormat(), e.getDocumentSigneTaille(),
                        e.getDocumentSigneDeposeLe(), e.getDocumentSigneDeposePar());
        return AutorisationPrelevement.reconstituer(e.getId(), e.getContratId(), e.getDemandeId(), e.getClientId(),
                e.getType(), e.getStatut(), e.getEmiseLe(), e.getStatutChangeLe(), document);
    }
}
