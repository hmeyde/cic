package com.cic.paiement.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.domain.Acteur;
import com.cic.audit.domain.EntreeJournal;
import com.cic.audit.domain.ObjetAudite;
import com.cic.commun.domain.Montant;
import com.cic.contrat.application.ContratService;
import com.cic.paiement.PaiementNonRapproche;
import com.cic.paiement.PaiementRapproche;
import com.cic.paiement.domain.Rattachement;
import com.cic.paiement.domain.Paiement;
import com.cic.paiement.domain.PaiementEnDoubleException;
import com.cic.paiement.domain.PaiementRepository;
import com.cic.paiement.domain.ReferenceApport;
import com.cic.paiement.domain.ReferenceEcheance;
import com.cic.paiement.domain.ReferencePaiement;

/**
 * Saisie manuelle des paiements reçus au back-office et rapprochement par référence.
 *
 * <p>La référence d'un paiement est unique : saisir une seconde fois un paiement de même référence
 * est refusé ({@link PaiementEnDoubleException}) sans rien enregistrer, ce qui rend la saisie
 * idempotente. Une contrainte d'unicité en base couvre deux saisies simultanées.
 *
 * <p>Rapprochement : une référence d'apport valide ({@link ReferenceApport}, clé juste) désigne le
 * contrat dont elle est tirée, donc son dossier ; une référence d'échéance valide
 * ({@link ReferenceEcheance}) désigne de même une échéance du contrat, qui passe à PAYEE si le montant
 * reçu la couvre. Le paiement est rattaché et l'événement {@link PaiementRapproche} est publié. Une
 * référence qui ne désigne aucun dossier n'empêche pas d'enregistrer l'argent reçu : le paiement reste
 * NON_RAPPROCHE et l'événement {@link PaiementNonRapproche} est publié. Chaque saisie est journalisée
 * (objet {@value #TYPE_OBJET}), dans la même transaction.
 */
@Service
public class PaiementService {

    static final String TYPE_OBJET = "PAIEMENT";
    static final String ACTION_SAISIE = "SAISIE_PAIEMENT";
    static final String ACTION_IMPORT = "IMPORT_PAIEMENT_CSV";

    /** Montant le plus élevé accepté à la saisie, contre une faute de frappe grossière. */
    static final int CHIFFRES_MONTANT_MAX = 9;

    private static final Logger LOG = LoggerFactory.getLogger(PaiementService.class);

    private final PaiementRepository paiements;
    private final ContratService contrats;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public PaiementService(PaiementRepository paiements, ContratService contrats, JournalAuditService journal,
            ApplicationEventPublisher evenements, Clock clock) {
        this.paiements = paiements;
        this.contrats = contrats;
        this.journal = journal;
        this.evenements = evenements;
        this.clock = clock;
    }

    static ObjetAudite objet(UUID paiementId) {
        return new ObjetAudite(TYPE_OBJET, paiementId);
    }

    /**
     * Enregistre un paiement reçu et le rapproche par sa référence.
     *
     * @param auteur identifiant de connexion du back-office
     * @throws SaisiePaiementInvalideException référence, montant ou date invalide
     * @throws PaiementEnDoubleException       un paiement porte déjà cette référence
     */
    @Transactional
    public Paiement saisir(SaisiePaiement saisie, String auteur) {
        return enregistrer(saisie, auteur, ACTION_SAISIE);
    }

    /**
     * Enregistre un paiement lu dans un fichier CSV importé : mêmes contrôles et même rapprochement
     * qu'une saisie manuelle, journalisé sous l'action {@value #ACTION_IMPORT}.
     *
     * @throws SaisiePaiementInvalideException référence, montant ou date invalide
     * @throws PaiementEnDoubleException       un paiement porte déjà cette référence
     */
    @Transactional
    public Paiement importer(SaisiePaiement saisie, String auteur) {
        return enregistrer(saisie, auteur, ACTION_IMPORT);
    }

    private Paiement enregistrer(SaisiePaiement saisie, String auteur, String action) {
        Objects.requireNonNull(saisie, "saisie");
        Objects.requireNonNull(auteur, "auteur");
        ReferencePaiement reference = reference(saisie.reference());
        Montant montant = montant(saisie.montant());
        LocalDate recuLe = date(saisie.recuLe());
        Optional<Paiement> existant = paiements.parReference(reference);
        if (existant.isPresent()) {
            LOG.info("Saisie refusée : {} porte déjà cette référence", existant.get());
            throw new PaiementEnDoubleException(existant.get().id());
        }
        Instant maintenant = clock.instant();
        Paiement paiement = Paiement.saisir(reference, montant, recuLe, rattachement(reference), auteur, maintenant);
        paiements.enregistrer(paiement);
        journal.journaliser(EntreeJournal.transition(maintenant, Acteur.backOffice(auteur), action,
                objet(paiement.id()), null, paiement.statut().name(),
                paiement.rattachement().map(Rattachement::libelle).orElse(null)));
        paiement.rattachement().ifPresentOrElse(a -> {
            if (!a.estApport() && montant.estSuperieurOuEgalA(a.montant())) {
                contrats.constaterEcheancePayee(a.contratId(), a.rangEcheance());
            }
            evenements.publishEvent(new PaiementRapproche(paiement.id(), a.demandeId(), a.contratId(),
                    a.numeroContrat(), a.clientId(), a.objet(), a.rangEcheance(), montant, a.montant(), recuLe,
                    maintenant));
        }, () -> evenements.publishEvent(new PaiementNonRapproche(paiement.id(), montant, recuLe, maintenant)));
        LOG.info("Paiement saisi : {}", paiement);
        return paiement;
    }

    /** Paiements en écart (référence inconnue, montant différent de l'apport), les plus récents d'abord. */
    @Transactional(readOnly = true)
    public List<Paiement> ecarts(int limite) {
        return paiements.ecarts(limite);
    }

    /** Paiements les plus récemment saisis d'abord. */
    @Transactional(readOnly = true)
    public List<Paiement> derniers(int limite) {
        return paiements.derniers(limite);
    }

    @Transactional(readOnly = true)
    public Optional<Paiement> parId(UUID id) {
        Objects.requireNonNull(id, "id");
        return paiements.parId(id);
    }

    /**
     * Total des paiements rattachés au dossier par sa référence d'apport ; zéro s'il n'en a reçu aucun.
     * L'apport est encaissé quand ce total atteint l'apport fixé au contrat.
     */
    @Transactional(readOnly = true)
    public Montant totalRecuPourDossier(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return paiements.totalApportRecuPourDossier(demandeId);
    }

    private Optional<Rattachement> rattachement(ReferencePaiement reference) {
        Optional<Rattachement> apport = reference.commeReferenceApport()
                .flatMap(r -> contrats.parChiffresDuNumero(r.chiffresContrat()))
                .map(c -> Rattachement.apport(c.id(), c.numero(), c.demandeId(), c.clientId(), c.apport()));
        if (apport.isPresent()) {
            return apport;
        }
        return reference.commeReferenceEcheance()
                .flatMap(r -> contrats.parChiffresDuNumero(r.chiffresContrat())
                        .flatMap(c -> c.echeance(r.rang()).map(e -> Rattachement.echeance(c.id(), c.numero(),
                                c.demandeId(), c.clientId(), e.rang(), e.montant()))));
    }

    private static ReferencePaiement reference(String saisie) {
        try {
            return ReferencePaiement.lire(saisie == null ? "" : saisie);
        } catch (IllegalArgumentException e) {
            throw new SaisiePaiementInvalideException(SaisiePaiementInvalideException.REFERENCE_INVALIDE);
        }
    }

    private static Montant montant(String saisie) {
        String chiffres = saisie == null ? "" : saisie.replaceAll("[\\s\\u00A0\\u202F]", "");
        if (!chiffres.matches("\\d{1," + CHIFFRES_MONTANT_MAX + "}")) {
            throw new SaisiePaiementInvalideException(SaisiePaiementInvalideException.MONTANT_INVALIDE);
        }
        Montant montant = Montant.de(Long.parseLong(chiffres));
        if (montant.estZero()) {
            throw new SaisiePaiementInvalideException(SaisiePaiementInvalideException.MONTANT_INVALIDE);
        }
        return montant;
    }

    private LocalDate date(String saisie) {
        LocalDate date;
        try {
            date = LocalDate.parse(saisie == null ? "" : saisie.strip());
        } catch (DateTimeParseException e) {
            throw new SaisiePaiementInvalideException(SaisiePaiementInvalideException.DATE_INVALIDE);
        }
        if (date.isAfter(LocalDate.now(clock))) {
            throw new SaisiePaiementInvalideException(SaisiePaiementInvalideException.DATE_INVALIDE);
        }
        return date;
    }
}
