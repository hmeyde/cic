package com.cic.comptabilite.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.domain.Acteur;
import com.cic.audit.domain.EntreeJournal;
import com.cic.audit.domain.ObjetAudite;
import com.cic.commun.domain.Montant;
import com.cic.comptabilite.domain.EcritureComptable;
import com.cic.comptabilite.domain.EcritureComptableRepository;
import com.cic.comptabilite.domain.EcritureDejaContrepasseeException;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.domain.Contrat;

/**
 * Passe les écritures comptables de chaque mouvement d'argent : apport encaissé, paiement
 * fournisseur, échéance encaissée, paiement reçu à identifier ; et la vente constatée à la remise du
 * produit, qui solde l'apport et ouvre le reste dû du client.
 *
 * <p>Idempotent : un fait (nature + source) n'est comptabilisé qu'une fois, même si l'événement qui
 * le signale est traité deux fois (registre d'événements rejoué). Une écriture n'est jamais modifiée :
 * une erreur se corrige par contrepassation, journalisée (objet {@value #TYPE_OBJET}).
 */
@Service
public class ComptabiliteService {

    static final String TYPE_OBJET = "ECRITURE_COMPTABLE";
    static final String ACTION_CONTREPASSATION = "CONTREPASSATION";

    private static final Logger LOG = LoggerFactory.getLogger(ComptabiliteService.class);

    private final EcritureComptableRepository ecritures;
    private final ContratService contrats;
    private final JournalAuditService journal;
    private final Clock clock;

    public ComptabiliteService(EcritureComptableRepository ecritures, ContratService contrats,
            JournalAuditService journal, Clock clock) {
        this.ecritures = ecritures;
        this.contrats = contrats;
        this.journal = journal;
        this.clock = clock;
    }

    static ObjetAudite objet(UUID ecritureId) {
        return new ObjetAudite(TYPE_OBJET, ecritureId);
    }

    /** Apport reçu sur la référence d'apport du dossier, pour le montant effectivement reçu. */
    @Transactional
    public void apportEncaisse(UUID paiementId, UUID demandeId, UUID contratId, String numeroContrat, Montant recu,
            LocalDate recuLe) {
        passer(EcritureComptable.apportEncaisse(paiementId, demandeId, contratId, numeroContrat, recu, recuLe,
                clock.instant()));
    }

    /** Échéance reçue sur sa référence d'échéance, pour le montant effectivement reçu. */
    @Transactional
    public void echeanceEncaissee(UUID paiementId, UUID demandeId, UUID contratId, String numeroContrat, int rang,
            Montant recu, LocalDate recuLe) {
        passer(EcritureComptable.echeanceEncaissee(paiementId, demandeId, contratId, numeroContrat, rang, recu,
                recuLe, clock.instant()));
    }

    /** Paiement reçu dont la référence ne désigne aucun dossier. */
    @Transactional
    public void paiementAIdentifier(UUID paiementId, Montant recu, LocalDate recuLe) {
        passer(EcritureComptable.paiementAIdentifier(paiementId, recu, recuLe, clock.instant()));
    }

    /** Prix d'achat payé au fournisseur, à la date de validation de la commande. */
    @Transactional
    public void paiementFournisseur(UUID commandeId, String numeroCommande, UUID demandeId, UUID contratId,
            Montant prixAchat, Instant valideLe) {
        passer(EcritureComptable.paiementFournisseur(commandeId, numeroCommande, demandeId, contratId, prixAchat,
                date(valideLe), clock.instant()));
    }

    /**
     * Vente du dossier constatée à la remise du produit, au prix de vente et à l'apport fixés au
     * contrat.
     *
     * @throws IllegalStateException le dossier n'a pas de contrat
     */
    @Transactional
    public void venteLivree(UUID demandeId, Instant livreLe) {
        Objects.requireNonNull(demandeId, "demandeId");
        Contrat contrat = contrats.parDemande(demandeId)
                .orElseThrow(() -> new IllegalStateException("Contrat introuvable pour le dossier " + demandeId));
        passer(EcritureComptable.venteLivree(demandeId, contrat.id(), contrat.numero(), contrat.prix().prixVente(),
                contrat.apport(), date(livreLe), clock.instant()));
    }

    /**
     * Annule une écriture par une écriture inverse, datée du jour. L'écriture d'origine reste.
     *
     * @param auteur identifiant de connexion du back-office
     * @throws EcritureIntrouvableException       écriture inconnue
     * @throws EcritureDejaContrepasseeException  écriture déjà contrepassée, ou elle-même contrepassation
     */
    @Transactional
    public EcritureComptable contrepasser(UUID ecritureId, String motif, String auteur) {
        Objects.requireNonNull(ecritureId, "ecritureId");
        Objects.requireNonNull(auteur, "auteur");
        EcritureComptable origine = ecritures.parId(ecritureId).orElseThrow(EcritureIntrouvableException::new);
        Instant maintenant = clock.instant();
        EcritureComptable inverse = origine.contrepasser(motif == null ? "" : motif, date(maintenant), maintenant);
        if (!ecritures.enregistrerSiNouvelle(inverse)) {
            throw new EcritureDejaContrepasseeException();
        }
        journal.journaliser(EntreeJournal.action(maintenant, Acteur.backOffice(auteur), ACTION_CONTREPASSATION,
                objet(origine.id()), "Contrepassée par l'écriture " + inverse.id()));
        LOG.info("Écriture {} contrepassée par {}", origine, inverse);
        return inverse;
    }

    /** Écritures du dossier, dans l'ordre où elles ont été passées. */
    @Transactional(readOnly = true)
    public List<EcritureComptable> duDossier(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return ecritures.parDemande(demandeId);
    }

    /** Écritures les plus récentes d'abord. */
    @Transactional(readOnly = true)
    public List<EcritureComptable> dernieres(int limite) {
        return ecritures.dernieres(limite);
    }

    private void passer(EcritureComptable ecriture) {
        if (ecritures.enregistrerSiNouvelle(ecriture)) {
            LOG.info("Écriture passée : {}", ecriture);
        } else {
            LOG.info("Déjà comptabilisé : {} de la source {}", ecriture.nature(), ecriture.sourceId());
        }
    }

    private LocalDate date(Instant le) {
        return LocalDate.ofInstant(Objects.requireNonNull(le, "le"), clock.getZone());
    }
}
