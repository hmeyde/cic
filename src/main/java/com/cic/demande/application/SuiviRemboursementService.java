package com.cic.demande.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.domain.Acteur;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Echeance;
import com.cic.demande.StatutDossierChange;
import com.cic.demande.domain.ChangementStatut;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandeRepository;
import com.cic.demande.domain.SituationEcheances;

/**
 * Décisions du recouvrement sur un dossier en remboursement, prises au back-office :
 *
 * <ul>
 *   <li>{@link #regulariser} : EN_RETARD → EN_REMBOURSEMENT, une fois les échéances en retard payées ;</li>
 *   <li>{@link #passerEnRecouvrement} : EN_RETARD → RECOUVREMENT, avec un motif ;</li>
 *   <li>{@link #solder} : EN_REMBOURSEMENT → SOLDEE, une fois toutes les échéances payées.</li>
 * </ul>
 *
 * Dans une seule transaction : dossier verrouillé, situation des échéances lue sur le contrat,
 * transition par la machine à états, entrée au journal d'audit (auteur connecté), événement
 * {@link StatutDossierChange}.
 */
@Service
public class SuiviRemboursementService {

    private static final Logger LOG = LoggerFactory.getLogger(SuiviRemboursementService.class);

    private final DemandeRepository demandes;
    private final ContratService contrats;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public SuiviRemboursementService(DemandeRepository demandes, ContratService contrats, JournalAuditService journal,
            ApplicationEventPublisher evenements, Clock clock) {
        this.demandes = demandes;
        this.contrats = contrats;
        this.journal = journal;
        this.evenements = evenements;
        this.clock = clock;
    }

    /**
     * @param auteur identifiant de connexion du back-office
     * @throws DossierIntrouvableException dossier inconnu ou sans contrat
     * @throws com.cic.demande.domain.TransitionInterditeException      si le dossier n'est pas EN_RETARD
     * @throws com.cic.demande.domain.RegularisationImpossibleException une échéance est encore EN_RETARD
     */
    @Transactional
    public Demande regulariser(UUID demandeId, String auteur) {
        return decider(demandeId, auteur, ActionDossier.REGULARISATION,
                (demande, situation) -> demande.regulariser(situation, clock.instant()),
                "Échéances en retard payées : prélèvements repris");
    }

    /**
     * @param auteur identifiant de connexion du back-office
     * @param motif  raison du passage en recouvrement, inscrite au journal du dossier
     * @throws DossierIntrouvableException dossier inconnu ou sans contrat
     * @throws com.cic.demande.domain.TransitionInterditeException       si le dossier n'est pas EN_RETARD
     * @throws com.cic.demande.domain.MotifRecouvrementInvalideException motif vide ou trop long
     */
    @Transactional
    public Demande passerEnRecouvrement(UUID demandeId, String auteur, String motif) {
        return decider(demandeId, auteur, ActionDossier.PASSAGE_EN_RECOUVREMENT,
                (demande, situation) -> demande.passerEnRecouvrement(motif, clock.instant()),
                motif == null ? null : motif.strip());
    }

    /**
     * @param auteur identifiant de connexion du back-office
     * @throws DossierIntrouvableException dossier inconnu ou sans contrat
     * @throws com.cic.demande.domain.TransitionInterditeException si le dossier n'est pas EN_REMBOURSEMENT
     * @throws com.cic.demande.domain.SoldeImpossibleException     une échéance reste à payer
     */
    @Transactional
    public Demande solder(UUID demandeId, String auteur) {
        return decider(demandeId, auteur, ActionDossier.SOLDE,
                (demande, situation) -> demande.solder(situation, clock.instant()),
                "Toutes les échéances sont payées");
    }

    /** Situation des échéances du contrat du dossier. */
    @Transactional(readOnly = true)
    public SituationEcheances situation(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return situation(contrats.parDemande(demandeId).orElseThrow(DossierIntrouvableException::new));
    }

    static SituationEcheances situation(Contrat contrat) {
        int enRetard = (int) contrat.echeances().stream().filter(Echeance::estEnRetard).count();
        int nonPayees = (int) contrat.echeances().stream().filter(e -> !e.estPayee()).count();
        return new SituationEcheances(enRetard, nonPayees);
    }

    private Demande decider(UUID demandeId, String auteur, ActionDossier action,
            BiFunction<Demande, SituationEcheances, ChangementStatut> transition, String detail) {
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(auteur, "auteur");
        Demande demande = demandes.parIdVerrouillee(demandeId).orElseThrow(DossierIntrouvableException::new);
        Contrat contrat = contrats.parDemande(demandeId).orElseThrow(DossierIntrouvableException::new);
        ChangementStatut changement = transition.apply(demande, situation(contrat));
        Instant le = demande.statutChangeLe();
        demandes.enregistrer(demande);
        journal.journaliser(JournalDossier.transition(demande.id(), le, Acteur.backOffice(auteur), action,
                changement.depuis(), changement.vers(), "Contrat " + contrat.numero() + " : " + detail));
        evenements.publishEvent(new StatutDossierChange(demande.id(), demande.clientId(), changement.depuis(),
                changement.vers(), le));
        LOG.info("{} : {}", action, demande);
        return demande;
    }
}
