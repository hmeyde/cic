package com.cic.contrat.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.domain.Acteur;
import com.cic.audit.domain.EntreeJournal;
import com.cic.audit.domain.ObjetAudite;
import com.cic.contrat.AutorisationPrelevementStatutChange;
import com.cic.contrat.domain.AutorisationPrelevement;
import com.cic.contrat.domain.AutorisationPrelevementRepository;
import com.cic.contrat.domain.ChangementStatutAutorisation;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.ContratRepository;
import com.cic.contrat.domain.DocumentSigne;
import com.cic.contrat.domain.StatutAutorisation;
import com.cic.contrat.domain.StockageDocumentsSignes;
import com.cic.identite.application.ClientService;
import com.cic.identite.domain.Client;
import com.cic.identite.domain.FormatDocument;

/**
 * Suivi des autorisations de prélèvement au back-office : liste par statut, fiche, dépôt du document
 * signé par le client (A_SIGNER → SIGNEE), transmission à la banque, acceptation, activation et
 * révocation.
 *
 * <p>Chaque action, dans une seule transaction : autorisation verrouillée, règle appliquée par le
 * domaine ({@link StatutAutorisation}), autorisation enregistrée, entrée au journal d'audit (objet
 * {@value #TYPE_OBJET}), événement {@link AutorisationPrelevementStatutChange} publié. Le document
 * signé suit les règles des justificatifs : PDF, JPEG ou PNG reconnu au contenu, 5 Mo au plus. Un
 * document remplacé reste stocké : il garde la trace de ce qui a été déposé.
 */
@Service
public class SuiviAutorisationService {

    static final String TYPE_OBJET = "AUTORISATION_PRELEVEMENT";

    /** Longueur maximale du motif d'une révocation. */
    public static final int LONGUEUR_MOTIF_MAX = 300;

    private static final Logger LOG = LoggerFactory.getLogger(SuiviAutorisationService.class);

    private final AutorisationPrelevementRepository autorisations;
    private final ContratRepository contrats;
    private final ClientService clients;
    private final StockageDocumentsSignes stockage;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public SuiviAutorisationService(AutorisationPrelevementRepository autorisations, ContratRepository contrats,
            ClientService clients, StockageDocumentsSignes stockage, JournalAuditService journal,
            ApplicationEventPublisher evenements, Clock clock) {
        this.autorisations = autorisations;
        this.contrats = contrats;
        this.clients = clients;
        this.stockage = stockage;
        this.journal = journal;
        this.evenements = evenements;
        this.clock = clock;
    }

    static ObjetAudite objet(UUID autorisationId) {
        return new ObjetAudite(TYPE_OBJET, autorisationId);
    }

    /**
     * Autorisations les plus récemment émises d'abord.
     *
     * @param statut filtre ; vide pour tous les statuts
     */
    @Transactional(readOnly = true)
    public List<LigneAutorisation> liste(Optional<StatutAutorisation> statut, int limite) {
        return autorisations.dernieres(statut, limite).stream().map(a -> {
            Contrat c = contrat(a);
            return new LigneAutorisation(a, c.numero(), c.designationProduit(),
                    clients.parId(a.clientId()).map(SuiviAutorisationService::nom).orElse(""));
        }).toList();
    }

    /** Fiche de suivi ; vide si l'autorisation n'existe pas. */
    @Transactional(readOnly = true)
    public Optional<FicheAutorisation> fiche(UUID autorisationId) {
        Objects.requireNonNull(autorisationId, "autorisationId");
        return autorisations.parId(autorisationId).map(a -> new FicheAutorisation(a, contrat(a),
                clients.parId(a.clientId()), journal.historique(objet(a.id()))));
    }

    /** Autorisation du dossier ; vide tant que le dossier n'a pas de contrat. */
    @Transactional(readOnly = true)
    public Optional<AutorisationPrelevement> parDemande(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return contrats.parDemande(demandeId).flatMap(c -> autorisations.parContrat(c.id()));
    }

    /** Contenu du document signé ; vide si l'autorisation n'existe pas ou n'a pas encore de document. */
    @Transactional(readOnly = true)
    public Optional<DocumentSigneContenu> documentSigne(UUID autorisationId) {
        Objects.requireNonNull(autorisationId, "autorisationId");
        Optional<DocumentSigneContenu> contenu = autorisations.parId(autorisationId)
                .flatMap(AutorisationPrelevement::documentSigne)
                .map(d -> new DocumentSigneContenu(d, stockage.lire(d.id())));
        contenu.ifPresent(c -> LOG.info("Autorisation signée {} consultée par le back-office", c.document().id()));
        return contenu;
    }

    /**
     * Dépose l'autorisation signée par le client : A_SIGNER → SIGNEE, ou remplacement du document
     * d'une autorisation SIGNEE.
     *
     * @param auteur identifiant de connexion du back-office
     * @throws AutorisationIntrouvableException    autorisation inconnue
     * @throws SaisieAutorisationInvalideException contenu vide, trop volumineux ou ni PDF, ni JPEG, ni PNG
     * @throws com.cic.contrat.domain.DepotDocumentSigneImpossibleException autorisation déjà transmise,
     *         acceptée, active ou révoquée
     */
    @Transactional
    public AutorisationPrelevement deposerDocumentSigne(UUID autorisationId, String auteur, byte[] contenu) {
        if (contenu == null || contenu.length == 0) {
            throw new SaisieAutorisationInvalideException(SaisieAutorisationInvalideException.DOCUMENT_VIDE);
        }
        if (contenu.length > DocumentSigne.TAILLE_MAX_OCTETS) {
            throw new SaisieAutorisationInvalideException(
                    SaisieAutorisationInvalideException.DOCUMENT_TROP_VOLUMINEUX);
        }
        FormatDocument format = FormatDocument.detecter(contenu)
                .orElseThrow(() -> new SaisieAutorisationInvalideException(
                        SaisieAutorisationInvalideException.FORMAT_REFUSE));
        AutorisationPrelevement autorisation = charger(autorisationId);
        Instant maintenant = clock.instant();
        DocumentSigne document = DocumentSigne.nouveau(format, contenu.length, maintenant, auteur);
        Optional<ChangementStatutAutorisation> changement = autorisation.deposerDocumentSigne(document);
        stockage.ecrire(document.id(), contenu);
        try {
            if (changement.isPresent()) {
                enregistrer(autorisation, changement.get(), auteur, ActionAutorisation.DEPOT_DOCUMENT_SIGNE,
                        null, maintenant);
            } else {
                autorisations.mettreAJour(autorisation);
                journal.journaliser(EntreeJournal.action(maintenant, Acteur.backOffice(auteur),
                        ActionAutorisation.REMPLACEMENT_DOCUMENT_SIGNE.name(), objet(autorisation.id()), null));
                LOG.info("Document signé remplacé : {}", autorisation);
            }
        } catch (RuntimeException e) {
            stockage.supprimer(document.id());
            throw e;
        }
        return autorisation;
    }

    /** SIGNEE → TRANSMISE : l'ordre signé est remis à la banque. */
    @Transactional
    public AutorisationPrelevement transmettre(UUID autorisationId, String auteur) {
        return transition(autorisationId, auteur, ActionAutorisation.TRANSMISSION, null,
                a -> a.transmettre(clock.instant()));
    }

    /** TRANSMISE → ACCEPTEE : la banque a visé l'ordre. */
    @Transactional
    public AutorisationPrelevement accepter(UUID autorisationId, String auteur) {
        return transition(autorisationId, auteur, ActionAutorisation.ACCEPTATION, null,
                a -> a.accepter(clock.instant()));
    }

    /** ACCEPTEE → ACTIVE : premier prélèvement constaté. */
    @Transactional
    public AutorisationPrelevement activer(UUID autorisationId, String auteur) {
        return transition(autorisationId, auteur, ActionAutorisation.ACTIVATION, null,
                a -> a.activer(clock.instant()));
    }

    /**
     * → REVOQUEE, avec un motif conservé au journal.
     *
     * @throws SaisieAutorisationInvalideException motif vide ou trop long
     */
    @Transactional
    public AutorisationPrelevement revoquer(UUID autorisationId, String auteur, String motif) {
        String texte = motif == null ? "" : motif.strip();
        if (texte.isEmpty() || texte.length() > LONGUEUR_MOTIF_MAX) {
            throw new SaisieAutorisationInvalideException(SaisieAutorisationInvalideException.MOTIF_INVALIDE);
        }
        return transition(autorisationId, auteur, ActionAutorisation.REVOCATION, texte,
                a -> a.revoquer(clock.instant()));
    }

    private AutorisationPrelevement transition(UUID autorisationId, String auteur, ActionAutorisation action,
            String detail, Function<AutorisationPrelevement, ChangementStatutAutorisation> regle) {
        AutorisationPrelevement autorisation = charger(autorisationId);
        ChangementStatutAutorisation changement = regle.apply(autorisation);
        enregistrer(autorisation, changement, auteur, action, detail, autorisation.statutChangeLe());
        return autorisation;
    }

    private AutorisationPrelevement charger(UUID autorisationId) {
        Objects.requireNonNull(autorisationId, "autorisationId");
        return autorisations.parIdVerrouillee(autorisationId).orElseThrow(AutorisationIntrouvableException::new);
    }

    private void enregistrer(AutorisationPrelevement a, ChangementStatutAutorisation changement, String auteur,
            ActionAutorisation action, String detail, Instant le) {
        autorisations.mettreAJour(a);
        journal.journaliser(EntreeJournal.transition(le, Acteur.backOffice(auteur), action.name(), objet(a.id()),
                changement.depuis().name(), changement.vers().name(), detail));
        evenements.publishEvent(new AutorisationPrelevementStatutChange(a.id(), a.demandeId(), a.clientId(),
                changement.depuis(), changement.vers(), le));
        LOG.info("Autorisation {} : {}", action, a);
    }

    private Contrat contrat(AutorisationPrelevement a) {
        return contrats.parId(a.contratId())
                .orElseThrow(() -> new IllegalStateException("Contrat introuvable pour " + a));
    }

    private static String nom(Client c) {
        return (c.prenom() + " " + c.nom()).strip();
    }
}
