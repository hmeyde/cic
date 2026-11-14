package com.seheli.demande.application;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.domain.Acteur;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Echeance;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.domain.ChangementStatut;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandeRepository;

/**
 * Confirmation par le client de la remise de son produit : COMMANDEE → LIVREE, puis aussitôt
 * LIVREE → EN_REMBOURSEMENT. Les échéances fixées au contrat à la signature (dates et montants) ne
 * changent pas : elles sont désormais prélevées le jour de paie du client.
 *
 * <p>Le dossier est toujours celui du client connecté (son dossier actif), jamais un identifiant reçu
 * de la requête. Dans une seule transaction : dossier verrouillé, deux transitions par la machine à
 * états, deux entrées au journal d'audit (la remise par le client, le démarrage du remboursement par
 * le système), deux événements {@link StatutDossierChange}. Une seconde confirmation est refusée par
 * la machine à états (le dossier n'est plus COMMANDEE).
 */
@Service
public class RemiseProduitService {

    /** Acteur système inscrit au journal pour le démarrage du remboursement. */
    public static final String TRAITEMENT = "remise-produit";

    private static final Logger LOG = LoggerFactory.getLogger(RemiseProduitService.class);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DemandeRepository demandes;
    private final ContratService contrats;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public RemiseProduitService(DemandeRepository demandes, ContratService contrats, JournalAuditService journal,
            ApplicationEventPublisher evenements, Clock clock) {
        this.demandes = demandes;
        this.contrats = contrats;
        this.journal = journal;
        this.evenements = evenements;
        this.clock = clock;
    }

    /**
     * Le client confirme avoir reçu son produit : le remboursement démarre.
     *
     * @param confirmation case « j'ai reçu mon produit » cochée
     * @throws RemiseNonConfirmeeException si la case n'est pas cochée
     * @throws DossierIntrouvableException si le client n'a pas de dossier actif
     * @throws com.seheli.demande.domain.TransitionInterditeException si le dossier n'est pas COMMANDEE
     */
    @Transactional
    public Demande confirmer(UUID clientId, boolean confirmation) {
        Objects.requireNonNull(clientId, "clientId");
        if (!confirmation) {
            throw new RemiseNonConfirmeeException();
        }
        UUID demandeId = demandes.parClient(clientId).stream().filter(Demande::estActive).findFirst()
                .map(Demande::id).orElseThrow(DossierIntrouvableException::new);
        Demande demande = demandes.parIdVerrouillee(demandeId).orElseThrow(DossierIntrouvableException::new);
        Instant maintenant = clock.instant();

        ChangementStatut remise = demande.confirmerRemise(maintenant);
        ChangementStatut remboursement = demande.demarrerRemboursement(maintenant);
        Contrat contrat = contrats.parDemande(demande.id())
                .orElseThrow(() -> new IllegalStateException("Contrat introuvable pour " + demande));
        demandes.enregistrer(demande);

        journal.journaliser(JournalDossier.transition(demande.id(), maintenant, Acteur.client(clientId),
                ActionDossier.CONFIRMATION_REMISE, remise.depuis(), remise.vers(),
                "Remise du produit confirmée par le client"));
        journal.journaliser(JournalDossier.transition(demande.id(), maintenant, Acteur.systeme(TRAITEMENT),
                ActionDossier.DEBUT_REMBOURSEMENT, remboursement.depuis(), remboursement.vers(),
                detailRemboursement(contrat)));
        evenements.publishEvent(new StatutDossierChange(demande.id(), demande.clientId(), remise.depuis(),
                remise.vers(), maintenant));
        evenements.publishEvent(new StatutDossierChange(demande.id(), demande.clientId(), remboursement.depuis(),
                remboursement.vers(), maintenant));
        LOG.info("Remise confirmée, remboursement démarré : {}", demande);
        return demande;
    }

    private static String detailRemboursement(Contrat contrat) {
        Echeance premiere = contrat.echeances().getFirst();
        return "Contrat " + contrat.numero() + " : " + contrat.nombreMois() + " échéances, première le "
                + premiere.date().format(JOUR) + " (" + premiere.montant().valeur().toPlainString() + " MRU)";
    }
}
