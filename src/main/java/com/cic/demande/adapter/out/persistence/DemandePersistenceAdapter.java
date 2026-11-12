package com.cic.demande.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cic.commun.domain.Montant;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandeRepository;
import com.cic.demande.domain.Eligibilite;
import com.cic.demande.domain.NoteScore;
import com.cic.demande.domain.ResultatCondition;
import com.cic.demande.domain.ResultatCritere;
import com.cic.demande.domain.Score;
import com.cic.demande.domain.StatutDossier;
import com.cic.demande.domain.TermesDemande;

import jakarta.persistence.EntityManager;

@Component
class DemandePersistenceAdapter implements DemandeRepository {

    private final DemandeJpaRepository demandes;
    private final ScoreJpaRepository scores;
    private final EntityManager entityManager;

    DemandePersistenceAdapter(DemandeJpaRepository demandes, ScoreJpaRepository scores, EntityManager entityManager) {
        this.demandes = demandes;
        this.scores = scores;
        this.entityManager = entityManager;
    }

    /** Nouvelle demande : demande et score insérés ; demande existante : seul le statut change. */
    @Override
    @Transactional
    public void enregistrer(Demande d) {
        Optional<DemandeEntity> existante = demandes.findById(d.id());
        if (existante.isPresent()) {
            existante.get().changerStatut(d.statut(), d.statutChangeLe());
            return;
        }
        entityManager.persist(versEntite(d));
        entityManager.persist(versScore(d));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Demande> parId(UUID id) {
        return demandes.findById(id).map(e -> versDomaine(e, scores.findById(e.getId()).orElseThrow()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Demande> parClient(UUID clientId) {
        return avecScores(demandes.findByClientIdOrderBySoumiseLeDesc(clientId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Demande> parStatutDeLaPlusAncienne(StatutDossier statut) {
        return avecScores(demandes.findByStatutOrderBySoumiseLeAscIdAsc(statut));
    }

    /** Scores chargés en une seule requête ; l'ordre des demandes est conservé. */
    private List<Demande> avecScores(List<DemandeEntity> entites) {
        if (entites.isEmpty()) {
            return List.of();
        }
        Map<UUID, ScoreEntity> parDemande = scores.findByDemandeIdIn(entites.stream().map(DemandeEntity::getId).toList())
                .stream().collect(Collectors.toMap(ScoreEntity::getDemandeId, Function.identity()));
        return entites.stream().map(e -> versDomaine(e, parDemande.get(e.getId()))).toList();
    }

    /**
     * Verrou consultatif PostgreSQL pris jusqu'à la fin de la transaction, sur une clé tirée de
     * l'identifiant du client (une collision entre deux clients ne fait que sérialiser leurs demandes).
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void verrouillerClient(UUID clientId) {
        long cle = clientId.getMostSignificantBits() ^ clientId.getLeastSignificantBits();
        entityManager.createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(?1)) verrou")
                .setParameter(1, cle)
                .getSingleResult();
    }

    private static DemandeEntity versEntite(Demande d) {
        TermesDemande t = d.termes();
        return new DemandeEntity(d.id(), d.clientId(), t.produitId(), t.fournisseurId(), t.designationProduit(),
                t.prixPublic().valeur(), t.prixAchat().valeur(), t.prixVente().valeur(), t.apportMinimum().valeur(),
                t.apport().valeur(), t.nombreMois(), t.mensualite().valeur(), t.derniereMensualite().valeur(),
                d.statut(), d.soumiseLe(), d.statutChangeLe());
    }

    private static ScoreEntity versScore(Demande d) {
        Score s = d.score();
        List<ScoreConditionEmbeddable> conditions = new ArrayList<>();
        List<ResultatCondition> resultats = s.eligibilite().resultats();
        for (int i = 0; i < resultats.size(); i++) {
            ResultatCondition r = resultats.get(i);
            conditions.add(new ScoreConditionEmbeddable(i + 1, r.condition(), r.satisfaite(), r.raison()));
        }
        List<ScoreCritereEmbeddable> criteres = new ArrayList<>();
        List<ResultatCritere> points = s.note().criteres();
        for (int i = 0; i < points.size(); i++) {
            ResultatCritere r = points.get(i);
            criteres.add(new ScoreCritereEmbeddable(i + 1, r.critere(), r.points(), r.pointsMax(), r.raison()));
        }
        return new ScoreEntity(d.id(), s.versionGrille(), s.eligibilite().estEligible(), s.note().note(),
                s.note().recommandation(), s.recommandation(), d.soumiseLe(), conditions, criteres);
    }

    private static Demande versDomaine(DemandeEntity e, ScoreEntity s) {
        TermesDemande termes = new TermesDemande(e.getProduitId(), e.getFournisseurId(), e.getDesignationProduit(),
                Montant.de(e.getPrixPublic()), Montant.de(e.getPrixAchat()), Montant.de(e.getPrixVente()),
                Montant.de(e.getApportMinimum()), Montant.de(e.getApport()), e.getNombreMois(),
                Montant.de(e.getMensualite()), Montant.de(e.getDerniereMensualite()));
        Score score = new Score(
                new Eligibilite(s.getConditions().stream()
                        .map(c -> new ResultatCondition(c.getCondition(), c.isSatisfaite(), c.getRaison())).toList()),
                new NoteScore(s.getVersionGrille(), s.getCriteres().stream()
                        .map(c -> new ResultatCritere(c.getCritere(), c.getPoints(), c.getRaison())).toList(),
                        s.getRecommandationNote()));
        return Demande.reconstituer(e.getId(), e.getClientId(), termes, score, e.getSoumiseLe(), e.getStatut(),
                e.getStatutChangeLe());
    }
}
