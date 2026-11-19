package com.seheli.comptabilite.adapter.out.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.query.NativeQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.commun.domain.Montant;
import com.seheli.comptabilite.domain.Compte;
import com.seheli.comptabilite.domain.EcritureComptable;
import com.seheli.comptabilite.domain.EcritureComptableRepository;
import com.seheli.comptabilite.domain.LigneEcriture;
import com.seheli.comptabilite.domain.NatureEcriture;
import com.seheli.comptabilite.domain.SoldesDossier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Insertion seule, par requêtes natives : l'en-tête est inséré avec {@code ON CONFLICT DO NOTHING} sur
 * la contrainte (nature, source), de sorte qu'un fait déjà comptabilisé est ignoré sans erreur et sans
 * rendre la transaction inutilisable ; les lignes ne sont insérées que si l'en-tête l'a été.
 * L'équilibre débit = crédit est recontrôlé en base à la validation (trigger différé).
 */
@Component
class EcritureComptablePersistenceAdapter implements EcritureComptableRepository {

    private final EcritureComptableJpaRepository ecritures;
    private final EntityManager entityManager;

    EcritureComptablePersistenceAdapter(EcritureComptableJpaRepository ecritures, EntityManager entityManager) {
        this.ecritures = ecritures;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean enregistrerSiNouvelle(EcritureComptable e) {
        int inseree = entityManager.createNativeQuery("""
                INSERT INTO ecriture_comptable (id, nature, source_id, date_ecriture, libelle, demande_id, contrat_id,
                    contrepasse_id, passee_le)
                VALUES (:id, :nature, :sourceId, :date, :libelle, :demandeId, :contratId, :contrepasseId, :passeeLe)
                ON CONFLICT ON CONSTRAINT ecriture_source_uk DO NOTHING
                """).unwrap(NativeQuery.class)
                .setParameter("id", e.id())
                .setParameter("nature", e.nature().name())
                .setParameter("sourceId", e.sourceId())
                .setParameter("date", e.date())
                .setParameter("libelle", e.libelle())
                .setParameter("demandeId", e.demandeId().orElse(null), UUID.class)
                .setParameter("contratId", e.contratId().orElse(null), UUID.class)
                .setParameter("contrepasseId", e.contrepasse().orElse(null), UUID.class)
                .setParameter("passeeLe", Timestamp.from(e.passeeLe()))
                .executeUpdate();
        if (inseree == 0) {
            return false;
        }
        List<LigneEcriture> lignes = e.lignes();
        for (int i = 0; i < lignes.size(); i++) {
            LigneEcriture l = lignes.get(i);
            entityManager.createNativeQuery("""
                    INSERT INTO ligne_ecriture (ecriture_id, position, compte, sens, montant)
                    VALUES (:ecritureId, :position, :compte, :sens, :montant)
                    """)
                    .setParameter("ecritureId", e.id())
                    .setParameter("position", i)
                    .setParameter("compte", l.compte().numero())
                    .setParameter("sens", l.sens().name())
                    .setParameter("montant", l.montant().valeur())
                    .executeUpdate();
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EcritureComptable> parId(UUID id) {
        return ecritures.findById(id).map(EcritureComptablePersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EcritureComptable> parSource(NatureEcriture nature, UUID sourceId) {
        return ecritures.findByNatureAndSourceId(nature, sourceId)
                .map(EcritureComptablePersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EcritureComptable> parDemande(UUID demandeId) {
        return ecritures.findByDemandeIdOrderByPasseeLeAscIdAsc(demandeId).stream()
                .map(EcritureComptablePersistenceAdapter::versDomaine).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoldesDossier> soldesParDossier() {
        return soldes(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoldesDossier> soldesParDossierAu(LocalDate date) {
        return soldes(Objects.requireNonNull(date, "date"));
    }

    /** Soldes par dossier : toutes les écritures, ou seulement celles datées au plus tard de {@code au}. */
    private List<SoldesDossier> soldes(LocalDate au) {
        Query requete = entityManager.createNativeQuery("""
                SELECT e.demande_id,
                       coalesce(sum(CASE WHEN l.compte = :banque
                                         THEN CASE WHEN l.sens = 'DEBIT' THEN l.montant ELSE -l.montant END END), 0),
                       coalesce(sum(CASE WHEN l.compte = :achats
                                         THEN CASE WHEN l.sens = 'DEBIT' THEN l.montant ELSE -l.montant END END), 0)
                  FROM ecriture_comptable e
                  JOIN ligne_ecriture l ON l.ecriture_id = e.id
                 WHERE e.demande_id IS NOT NULL
                """ + (au == null ? "" : "   AND e.date_ecriture <= :au\n") + """
                 GROUP BY e.demande_id
                """)
                .setParameter("banque", Compte.BANQUE.numero())
                .setParameter("achats", Compte.ACHATS.numero());
        if (au != null) {
            requete.setParameter("au", au);
        }
        @SuppressWarnings("unchecked")
        List<Object[]> lignes = requete.getResultList();
        return lignes.stream().map(l -> new SoldesDossier((UUID) l[0], (BigDecimal) l[1], (BigDecimal) l[2]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EcritureComptable> dernieres(int limite) {
        return ecritures.findAllByOrderByDateEcritureDescPasseeLeDesc(PageRequest.of(0, limite)).stream()
                .map(EcritureComptablePersistenceAdapter::versDomaine).toList();
    }

    private static EcritureComptable versDomaine(EcritureComptableEntity e) {
        return EcritureComptable.reconstituer(e.getId(), e.getNature(), e.getSourceId(), e.getDateEcriture(),
                e.getLibelle(), e.getDemandeId(), e.getContratId(),
                e.getLignes().stream().map(l -> new LigneEcriture(Compte.deNumero(l.getCompte()), l.getSens(),
                        Montant.de(l.getMontant()))).toList(),
                e.getPasseeLe());
    }
}
