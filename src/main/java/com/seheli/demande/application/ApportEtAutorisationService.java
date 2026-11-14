package com.seheli.demande.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.domain.Acteur;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.domain.ChangementStatut;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandeRepository;
import com.seheli.demande.domain.PrealablesCommande;
import com.seheli.demande.domain.StatutDossier;

/**
 * Passage d'un dossier ACCEPTEE à APPORT_ET_AUTORISATION dès que l'apport est encaissé et que
 * l'autorisation de prélèvement est ACCEPTEE, dans l'ordre où ils arrivent. Appelé à la réception
 * des événements des modules paiement et contrat, et par le traitement d'expiration avant
 * d'annuler un dossier (filet de sécurité si un événement n'a pas été traité).
 *
 * <p>Dans une seule transaction : dossier verrouillé puis relu, préalables établis, transition par
 * la machine à états, entrée au journal d'audit (acteur système), événement publié. Idempotent :
 * un dossier qui n'est plus ACCEPTEE ou à qui il manque un préalable est laissé tel quel.
 */
@Service
public class ApportEtAutorisationService {

    /** Acteur système inscrit au journal d'audit. */
    public static final String TRAITEMENT = "apport-et-autorisation";

    private static final Logger LOG = LoggerFactory.getLogger(ApportEtAutorisationService.class);

    private final DemandeRepository demandes;
    private final PrealablesCommandeService prealables;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public ApportEtAutorisationService(DemandeRepository demandes, PrealablesCommandeService prealables,
            JournalAuditService journal, ApplicationEventPublisher evenements, Clock clock) {
        this.demandes = demandes;
        this.prealables = prealables;
        this.journal = journal;
        this.evenements = evenements;
        this.clock = clock;
    }

    /**
     * ACCEPTEE → APPORT_ET_AUTORISATION si l'apport est encaissé et l'autorisation ACCEPTEE.
     *
     * @return {@code true} si le dossier a changé de statut
     */
    @Transactional
    public boolean constaterSiReunis(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        Demande demande = demandes.parIdVerrouillee(demandeId).orElse(null);
        if (demande == null || demande.statut() != StatutDossier.ACCEPTEE) {
            return false;
        }
        PrealablesCommande etat = prealables.pour(demandeId);
        if (!etat.sontReunis()) {
            LOG.debug("Dossier {} : apport encaissé {}, autorisation acceptée {}", demande.id(),
                    etat.apportEncaisse(), etat.autorisationAcceptee());
            return false;
        }
        Instant maintenant = clock.instant();
        ChangementStatut changement = demande.constaterApportEtAutorisation(etat, maintenant);
        demandes.enregistrer(demande);
        journal.journaliser(JournalDossier.transition(demande.id(), maintenant, Acteur.systeme(TRAITEMENT),
                ActionDossier.APPORT_ET_AUTORISATION_RECUS, changement.depuis(), changement.vers(),
                "Apport encaissé et autorisation de prélèvement acceptée"));
        evenements.publishEvent(new StatutDossierChange(demande.id(), demande.clientId(), changement.depuis(),
                changement.vers(), maintenant));
        LOG.info("Apport et autorisation reçus : {}", demande);
        return true;
    }
}
