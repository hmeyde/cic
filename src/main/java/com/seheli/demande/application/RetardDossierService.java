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
import com.seheli.demande.domain.StatutDossier;

/**
 * Passage d'un dossier EN_REMBOURSEMENT à EN_RETARD quand le recouvrement constate une échéance
 * impayée à sa date. Dossier verrouillé puis relu ; transition par la machine à états, entrée au
 * journal d'audit (acteur système), événement {@link StatutDossierChange} publié.
 *
 * <p>Un dossier qui n'est pas EN_REMBOURSEMENT n'est pas modifié : déjà EN_RETARD ou en
 * RECOUVREMENT, ou pas encore livré (la machine à états n'autorise EN_RETARD que depuis
 * EN_REMBOURSEMENT). Dans ce dernier cas l'échéance reste EN_RETARD et suffit à bloquer les
 * nouveaux achats du client ; le dossier passera EN_RETARD au passage suivant du traitement une
 * fois le remboursement démarré.
 */
@Service
public class RetardDossierService {

    /** Acteur système inscrit au journal d'audit. */
    public static final String TRAITEMENT = "detection-retards";

    private static final Logger LOG = LoggerFactory.getLogger(RetardDossierService.class);

    private final DemandeRepository demandes;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public RetardDossierService(DemandeRepository demandes, JournalAuditService journal,
            ApplicationEventPublisher evenements, Clock clock) {
        this.demandes = demandes;
        this.journal = journal;
        this.evenements = evenements;
        this.clock = clock;
    }

    /**
     * EN_REMBOURSEMENT → EN_RETARD.
     *
     * @param detail motif inscrit au journal d'audit, sans donnée personnelle (rang, date, contrat)
     * @return {@code true} si le dossier vient de passer EN_RETARD ; {@code false} s'il est inconnu ou
     *         n'est pas EN_REMBOURSEMENT
     */
    @Transactional
    public boolean constaterRetard(UUID demandeId, String detail) {
        Objects.requireNonNull(demandeId, "demandeId");
        Demande demande = demandes.parIdVerrouillee(demandeId).orElse(null);
        if (demande == null || demande.statut() != StatutDossier.EN_REMBOURSEMENT) {
            return false;
        }
        Instant maintenant = clock.instant();
        ChangementStatut changement = demande.constaterRetard(maintenant);
        demandes.enregistrer(demande);
        journal.journaliser(JournalDossier.transition(demande.id(), maintenant, Acteur.systeme(TRAITEMENT),
                ActionDossier.PASSAGE_EN_RETARD, changement.depuis(), changement.vers(), detail));
        evenements.publishEvent(new StatutDossierChange(demande.id(), demande.clientId(), changement.depuis(),
                changement.vers(), maintenant));
        LOG.info("Dossier en retard : {}", demande);
        return true;
    }
}
