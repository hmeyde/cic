package com.cic.demande.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.domain.Acteur;
import com.cic.demande.StatutDossierChange;
import com.cic.demande.domain.ChangementStatut;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandeRepository;
import com.cic.demande.domain.StatutDossier;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;

/**
 * Annulation des dossiers ACCEPTEE dont l'apport ou l'autorisation de prélèvement manque encore
 * {@code delai.apport.autorisation.jours} jours après l'acceptation (7 au départ). Un dossier qui a
 * reçu les deux est déjà passé à APPORT_ET_AUTORISATION : rester ACCEPTEE suffit à dire qu'il en manque un.
 *
 * <p>Chaque dossier est annulé dans sa propre transaction ({@link #annulerSiDelaiDepasse}) : dossier
 * verrouillé puis relu, pour ne pas annuler un dossier régularisé entre la recherche et l'annulation ;
 * transition par la machine à états, entrée au journal d'audit (acteur système), événement publié.
 */
@Service
public class ExpirationDossiersService {

    /** Acteur système inscrit au journal d'audit. */
    public static final String TRAITEMENT = "expiration-dossiers";

    private static final Logger LOG = LoggerFactory.getLogger(ExpirationDossiersService.class);

    private final DemandeRepository demandes;
    private final ParametreService parametres;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public ExpirationDossiersService(DemandeRepository demandes, ParametreService parametres,
            JournalAuditService journal, ApplicationEventPublisher evenements, Clock clock) {
        this.demandes = demandes;
        this.parametres = parametres;
        this.journal = journal;
        this.evenements = evenements;
        this.clock = clock;
    }

    /** Dossiers ACCEPTEE depuis le délai ou plus, du plus ancien au plus récent. */
    @Transactional(readOnly = true)
    public List<UUID> dossiersADelaiDepasse() {
        return demandes.idsParStatutDepuisAuPlusTard(StatutDossier.ACCEPTEE, clock.instant().minus(delai()));
    }

    /**
     * ACCEPTEE → ANNULEE si le délai est toujours dépassé une fois le dossier verrouillé.
     *
     * @return {@code true} si le dossier a été annulé ; {@code false} s'il est inconnu, n'est plus
     *         ACCEPTEE ou n'a pas atteint le délai
     */
    @Transactional
    public boolean annulerSiDelaiDepasse(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        Demande demande = demandes.parIdVerrouillee(demandeId).orElse(null);
        Instant maintenant = clock.instant();
        Duration delai = delai();
        if (demande == null || !demande.delaiApportEtAutorisationDepasse(delai, maintenant)) {
            return false;
        }
        ChangementStatut changement = demande.annulerFauteApportOuAutorisation(delai, maintenant);
        demandes.enregistrer(demande);
        journal.journaliser(JournalDossier.transition(demande.id(), maintenant, Acteur.systeme(TRAITEMENT),
                ActionDossier.ANNULATION_DELAI_DEPASSE, changement.depuis(), changement.vers(),
                "Apport ou autorisation de prélèvement non reçu " + delai.toDays() + " jours après l'acceptation"));
        evenements.publishEvent(new StatutDossierChange(demande.id(), demande.clientId(), changement.depuis(),
                changement.vers(), maintenant));
        LOG.info("Dossier annulé, délai de l'apport et de l'autorisation dépassé : {}", demande);
        return true;
    }

    private Duration delai() {
        return Duration.ofDays(parametres.entier(CleParametre.DELAI_APPORT_AUTORISATION_JOURS));
    }
}
