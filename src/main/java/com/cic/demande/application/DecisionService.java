package com.cic.demande.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.domain.Acteur;
import com.cic.demande.JustificatifDemande;
import com.cic.demande.StatutDossierChange;
import com.cic.demande.domain.ChangementStatut;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandeRepository;
import com.cic.identite.domain.TypeDocument;

/**
 * Décision de l'analyste sur un dossier EN_ANALYSE : accepter, refuser avec un motif ou demander un
 * justificatif. Le score recommande, l'analyste décide : la décision n'est pas bornée par la
 * recommandation.
 *
 * <p>Chaque décision, dans une seule transaction : dossier verrouillé (deux analystes ne décident
 * pas en même temps), règle appliquée par la machine à états du domaine, dossier enregistré,
 * entrée au journal d'audit, événement publié.
 */
@Service
public class DecisionService {

    /** Longueur maximale de la précision jointe à une demande de justificatif. */
    public static final int LONGUEUR_PRECISION_MAX = 300;

    private static final Logger LOG = LoggerFactory.getLogger(DecisionService.class);

    private final DemandeRepository demandes;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public DecisionService(DemandeRepository demandes, JournalAuditService journal,
            ApplicationEventPublisher evenements, Clock clock) {
        this.demandes = demandes;
        this.journal = journal;
        this.evenements = evenements;
        this.clock = clock;
    }

    /**
     * EN_ANALYSE → ACCEPTEE.
     *
     * @param analyste identifiant de connexion du back-office
     * @throws DossierIntrouvableException dossier inconnu
     * @throws com.cic.demande.domain.TransitionInterditeException dossier pas EN_ANALYSE
     */
    @Transactional
    public Demande accepter(UUID demandeId, String analyste) {
        Demande demande = charger(demandeId);
        Instant maintenant = clock.instant();
        ChangementStatut changement = demande.accepter(analyste, maintenant);
        transitionEnregistree(demande, changement, analyste, ActionDossier.ACCEPTATION, null, maintenant);
        return demande;
    }

    /**
     * EN_ANALYSE → REFUSEE, avec un motif conservé au dossier et au journal.
     *
     * @throws DossierIntrouvableException dossier inconnu
     * @throws com.cic.demande.domain.TransitionInterditeException dossier pas EN_ANALYSE
     * @throws com.cic.demande.domain.MotifRefusInvalideException  motif vide ou trop long
     */
    @Transactional
    public Demande refuser(UUID demandeId, String analyste, String motif) {
        Demande demande = charger(demandeId);
        Instant maintenant = clock.instant();
        ChangementStatut changement = demande.refuser(analyste, motif, maintenant);
        transitionEnregistree(demande, changement, analyste, ActionDossier.REFUS,
                demande.decision().flatMap(d -> d.motif()).orElseThrow(), maintenant);
        return demande;
    }

    /**
     * Demande un justificatif au client ; le dossier reste EN_ANALYSE.
     *
     * @param precision précision pour le client (par exemple « bulletin d'octobre lisible ») ; peut être vide
     * @throws DossierIntrouvableException              dossier inconnu
     * @throws com.cic.demande.domain.DecisionImpossibleException dossier pas EN_ANALYSE
     * @throws PrecisionJustificatifTropLongueException précision trop longue
     */
    @Transactional
    public Demande demanderJustificatif(UUID demandeId, String analyste, TypeDocument type, String precision) {
        Objects.requireNonNull(type, "type");
        String texte = precision == null ? "" : precision.strip();
        if (texte.length() > LONGUEUR_PRECISION_MAX) {
            throw new PrecisionJustificatifTropLongueException();
        }
        Demande demande = charger(demandeId);
        demande.demanderJustificatif();
        Instant maintenant = clock.instant();
        String detail = libelle(type) + (texte.isEmpty() ? "" : " — " + texte);
        journal.journaliser(JournalDossier.action(demande.id(), maintenant, Acteur.backOffice(analyste),
                ActionDossier.DEMANDE_JUSTIFICATIF, detail));
        evenements.publishEvent(new JustificatifDemande(demande.id(), demande.clientId(), type, maintenant));
        LOG.info("Justificatif {} demandé : {}", type, demande);
        return demande;
    }

    private Demande charger(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return demandes.parIdVerrouillee(demandeId).orElseThrow(DossierIntrouvableException::new);
    }

    private void transitionEnregistree(Demande demande, ChangementStatut changement, String analyste,
            ActionDossier action, String detail, Instant le) {
        demandes.enregistrer(demande);
        journal.journaliser(JournalDossier.transition(demande.id(), le, Acteur.backOffice(analyste), action,
                changement.depuis(), changement.vers(), detail));
        evenements.publishEvent(new StatutDossierChange(demande.id(), demande.clientId(), changement.depuis(),
                changement.vers(), le));
        LOG.info("Décision {} : {}", action, demande);
    }

    /** Libellé de la pièce dans le détail du journal, texte libre en français comme les PDF du MVP. */
    static String libelle(TypeDocument type) {
        return switch (type) {
            case PIECE_IDENTITE -> "Pièce d'identité";
            case CONTRAT_OU_ATTESTATION -> "Contrat de travail ou attestation";
            case BULLETIN_PAIE -> "Bulletin de paie";
            case RIB -> "RIB";
        };
    }
}
