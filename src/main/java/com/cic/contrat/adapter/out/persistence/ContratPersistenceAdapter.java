package com.cic.contrat.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.ContratRepository;
import com.cic.contrat.domain.Echeance;
import com.cic.contrat.domain.EcheanceAPrelever;
import com.cic.contrat.domain.Prix;
import com.cic.contrat.domain.StatutEcheance;

import jakarta.persistence.EntityManager;

@Component
class ContratPersistenceAdapter implements ContratRepository {

    private final ContratJpaRepository contrats;
    private final EntityManager entityManager;

    ContratPersistenceAdapter(ContratJpaRepository contrats, EntityManager entityManager) {
        this.contrats = contrats;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void enregistrer(Contrat c) {
        Prix p = c.prix();
        ContratEntity entite = new ContratEntity(c.id(), c.numero(), c.demandeId(), c.clientId(), c.produitId(),
                c.fournisseurId(), c.designationProduit(), p.prixPublic().valeur(), p.prixAchat().valeur(),
                p.prixVente().valeur(), p.apportMinimum().valeur(), c.apport().valeur(), c.nombreMois(),
                c.etabliLe());
        for (Echeance e : c.echeances()) {
            entite.ajouter(new EcheanceEntity(e.id(), entite, e.rang(), e.date(), e.montant().valeur(), e.statut()));
        }
        entityManager.persist(entite);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contrat> parId(UUID id) {
        return contrats.findWithEcheancesById(id).map(ContratPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contrat> parDemande(UUID demandeId) {
        return contrats.findByDemandeId(demandeId).map(ContratPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contrat> parNumero(String numero) {
        return contrats.findByNumero(numero).map(ContratPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrerStatut(UUID contratId, Echeance echeance) {
        EcheanceEntity entite = entityManager.find(EcheanceEntity.class, echeance.id());
        if (entite == null || !entite.getContratId().equals(contratId)) {
            throw new IllegalStateException("Échéance introuvable : " + echeance);
        }
        entite.changerStatut(echeance.statut());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EcheanceAPrelever> echeancesEntre(StatutEcheance statut, LocalDate du, LocalDate au) {
        return contrats.findAvecEcheanceEntre(statut, du, au).stream()
                .map(ContratPersistenceAdapter::versDomaine)
                .flatMap(c -> c.echeances().stream()
                        .filter(e -> e.statut() == statut && !e.date().isBefore(du)
                                && !e.date().isAfter(au))
                        .map(e -> EcheanceAPrelever.de(c, e)))
                .sorted(Comparator.comparing((EcheanceAPrelever e) -> e.echeance().date())
                        .thenComparing(EcheanceAPrelever::numeroContrat))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean clientAUneEcheanceEnRetard(UUID clientId) {
        return contrats.existeEcheanceDuClient(clientId, StatutEcheance.EN_RETARD);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long prochainNumeroOrdre() {
        return ((Number) entityManager.createNativeQuery("SELECT nextval('contrat_numero_seq')").getSingleResult())
                .longValue();
    }

    private static Contrat versDomaine(ContratEntity e) {
        Prix prix = new Prix(Montant.de(e.getPrixPublic()), Montant.de(e.getPrixAchat()), Montant.de(e.getPrixVente()),
                Montant.de(e.getApportMinimum()));
        return Contrat.reconstituer(e.getId(), e.getNumero(), e.getDemandeId(), e.getClientId(), e.getProduitId(),
                e.getFournisseurId(), e.getDesignationProduit(), prix, Montant.de(e.getApport()),
                e.getEcheances().stream().map(x -> Echeance.reconstituer(x.getId(), x.getRang(), x.getDateEcheance(),
                        Montant.de(x.getMontant()), x.getStatut())).toList(),
                e.getEtabliLe());
    }
}
