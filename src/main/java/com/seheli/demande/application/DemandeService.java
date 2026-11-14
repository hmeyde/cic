package com.seheli.demande.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.domain.Acteur;
import com.seheli.catalogue.application.CatalogueClientService;
import com.seheli.catalogue.application.ConditionsProduit;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.EcheancierService;
import com.seheli.contrat.domain.Echeancier;
import com.seheli.contrat.domain.Prix;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.domain.ChangementStatut;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandeRepository;
import com.seheli.demande.domain.DossierEligibilite;
import com.seheli.demande.domain.DossierScore;
import com.seheli.demande.domain.ModeRemboursement;
import com.seheli.demande.domain.Score;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.demande.domain.SuiviDossier;
import com.seheli.demande.domain.TermesDemande;
import com.seheli.demande.domain.TypeContrat;
import com.seheli.identite.application.ClientService;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.Emploi;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;

/**
 * Demandes de crédit des clients : soumission et consultation.
 *
 * <p>Soumission, dans une seule transaction :
 * <ol>
 *   <li>client en retard (dossier EN_RETARD ou RECOUVREMENT, ou échéance EN_RETARD sur l'un de ses
 *       contrats) : nouveaux achats bloqués ;</li>
 *   <li>au plus {@code dossier.actif.max} (1) dossier actif par client ;</li>
 *   <li>produit proposé, durée dans les bornes, apport ≥ apport minimum et inférieur au prix de vente ;</li>
 *   <li>échéancier au jour de paie du client à partir de la date du jour : pour un CDD la dernière
 *       échéance tombe avant la fin du contrat, et le total des mensualités Seheli ne dépasse pas
 *       {@code endettement.max.fraction} (1/3) du salaire net ; sinon la demande est refusée d'emblée ;</li>
 *   <li>score v1 calculé et stocké avec la demande ;</li>
 *   <li>DEMANDE_SOUMISE puis EN_ANALYSE par la machine à états ; chaque statut est inscrit au
 *       journal d'audit et publié ({@link StatutDossierChange}).</li>
 * </ol>
 */
@Service
public class DemandeService {

    private static final Logger LOG = LoggerFactory.getLogger(DemandeService.class);

    private final DemandeRepository demandes;
    private final ClientService clients;
    private final EmploiClientService emplois;
    private final CatalogueClientService catalogue;
    private final EcheancierService echeancier;
    private final ContratService contrats;
    private final ScoreService score;
    private final ParametreService parametres;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public DemandeService(DemandeRepository demandes, ClientService clients, EmploiClientService emplois,
            CatalogueClientService catalogue, EcheancierService echeancier, ContratService contrats,
            ScoreService score, ParametreService parametres, JournalAuditService journal,
            ApplicationEventPublisher evenements, Clock clock) {
        this.contrats = contrats;
        this.journal = journal;
        this.demandes = demandes;
        this.clients = clients;
        this.emplois = emplois;
        this.catalogue = catalogue;
        this.echeancier = echeancier;
        this.score = score;
        this.parametres = parametres;
        this.evenements = evenements;
        this.clock = clock;
    }

    /**
     * Soumet une demande du client pour un produit du catalogue.
     *
     * @param apportPropose apport choisi par le client ; vide pour l'apport minimum
     * @return la demande enregistrée, au statut EN_ANALYSE, avec son score
     * @throws ClientEnRetardException       le client a une échéance impayée chez Seheli
     * @throws DossierActifExistantException le client a déjà un dossier actif
     * @throws EmploiNonDeclareException     le client n'a pas déclaré son emploi
     * @throws ProduitNonProposeException    produit inconnu, indisponible ou non proposé
     * @throws DureeInvalideException        durée hors bornes
     * @throws ApportInvalideException       apport inférieur au minimum ou couvrant tout le prix
     * @throws com.seheli.contrat.domain.FinContratDepasseeException CDD terminé avant la dernière échéance
     * @throws com.seheli.contrat.domain.EndettementDepasseException mensualités au-delà du tiers du salaire
     * @throws IllegalArgumentException      client inconnu
     */
    @Transactional
    public Demande soumettre(UUID clientId, UUID produitId, int nombreMois, Optional<Montant> apportPropose) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(produitId, "produitId");
        Objects.requireNonNull(apportPropose, "apportPropose");
        Client client = clients.parId(clientId).orElseThrow(() -> new IllegalArgumentException("Client inconnu"));

        demandes.verrouillerClient(clientId);
        List<Demande> existantes = demandes.parClient(clientId);
        if (existantes.stream().anyMatch(d -> d.statut().estEnRetard()) || contrats.clientAUneEcheanceEnRetard(clientId)) {
            throw new ClientEnRetardException();
        }
        List<Demande> actives = existantes.stream().filter(Demande::estActive).toList();
        if (actives.size() >= parametres.entier(CleParametre.DOSSIER_ACTIF_MAX)) {
            throw new DossierActifExistantException();
        }

        Emploi emploi = emplois.parClient(clientId).orElseThrow(EmploiNonDeclareException::new);
        ConditionsProduit produit = catalogue.conditions(produitId).orElseThrow(ProduitNonProposeException::new);
        if (!echeancier.dureeAutorisee().autorise(nombreMois)) {
            throw new DureeInvalideException();
        }
        Prix prix = produit.prix();
        Montant apport = apportPropose.orElse(prix.apportMinimum());
        if (!prix.accepteApport(apport) || apport.compareTo(prix.prixVente()) == 0) {
            throw new ApportInvalideException();
        }

        Montant mensualitesEnCours = actives.stream().map(d -> d.termes().mensualiteMaximale())
                .reduce(Montant.ZERO, Montant::plus);
        LocalDate aujourdhui = LocalDate.now(clock);
        Echeancier plan;
        try {
            plan = echeancier.etablir(prix.resteAFinancer(apport), nombreMois, aujourdhui, emploi.jourPaie(),
                    emploi.dateFin(), emploi.salaireNet(), mensualitesEnCours);
        } catch (IllegalArgumentException e) {
            // Reste trop petit pour être réparti sur cette durée : l'apport est trop proche du prix.
            throw new ApportInvalideException();
        }

        TermesDemande termes = new TermesDemande(produit.produitId(), produit.fournisseurId(),
                produit.designation(), prix.prixPublic(), prix.prixAchat(), prix.prixVente(), prix.apportMinimum(),
                apport, nombreMois, plan.mensualite(), plan.derniere().montant());
        Score evalue = score.evaluer(
                eligibilite(client, emploi, mensualitesEnCours, plan, existantes),
                dossierScore(emploi, mensualitesEnCours, plan, prix, apport, existantes));

        Instant maintenant = clock.instant();
        Demande demande = Demande.soumettre(clientId, termes, evalue, maintenant);
        StatutDossier initial = demande.statut();
        ChangementStatut analyse = demande.mettreEnAnalyse(maintenant);
        demandes.enregistrer(demande);
        Acteur acteur = Acteur.client(clientId);
        journal.journaliser(JournalDossier.transition(demande.id(), maintenant, acteur, ActionDossier.SOUMISSION,
                null, initial, null));
        journal.journaliser(JournalDossier.transition(demande.id(), maintenant, acteur,
                ActionDossier.MISE_EN_ANALYSE, analyse.depuis(), analyse.vers(), null));

        evenements.publishEvent(new StatutDossierChange(demande.id(), clientId, null, initial, maintenant));
        evenements.publishEvent(new StatutDossierChange(demande.id(), clientId, analyse.depuis(), analyse.vers(),
                maintenant));
        LOG.info("Demande soumise : {}, score {} ({})", demande, evalue.note().note(), evalue.recommandation());
        return demande;
    }

    /** Demandes du client, de la plus récente à la plus ancienne. */
    @Transactional(readOnly = true)
    public List<Demande> parClient(UUID clientId) {
        Objects.requireNonNull(clientId, "clientId");
        return demandes.parClient(clientId);
    }

    /**
     * File des analystes : dossiers EN_ANALYSE, du plus ancien au plus récent (date de soumission),
     * pour traiter d'abord les clients qui attendent depuis le plus longtemps.
     */
    @Transactional(readOnly = true)
    public List<DossierEnAnalyse> fileAnalyse() {
        return demandes.parStatutDeLaPlusAncienne(StatutDossier.EN_ANALYSE).stream()
                .map(d -> new DossierEnAnalyse(d, clients.parId(d.clientId())
                        .map(c -> (c.prenom() + " " + c.nom()).strip()).orElse("")))
                .toList();
    }

    /**
     * Suivi du dossier en cours du client : son dossier actif s'il en a un, sinon sa demande la plus
     * récente (refusée, annulée ou soldée) ; vide s'il n'a jamais fait de demande.
     */
    @Transactional(readOnly = true)
    public Optional<SuiviDossier> suiviDossierCourant(UUID clientId) {
        Objects.requireNonNull(clientId, "clientId");
        List<Demande> existantes = demandes.parClient(clientId);
        return existantes.stream().filter(Demande::estActive).findFirst()
                .or(() -> existantes.stream().findFirst())
                .map(d -> SuiviDossier.de(d, clock.getZone(),
                        parametres.entier(CleParametre.DELAI_APPORT_AUTORISATION_JOURS)));
    }

    private static DossierEligibilite eligibilite(Client client, Emploi emploi, Montant mensualitesEnCours,
            Echeancier plan, List<Demande> existantes) {
        return new DossierEligibilite(client.estNniVerifie(), client.estPieceIdentiteVerifiee(),
                client.dateNaissance(), emploi.dateDebut(), emploi.dateFin(), emploi.estSalaireVerseSurCompte(),
                emploi.salaireNet(), mensualitesEnCours, plan.mensualiteMaximale(),
                existantes.stream().anyMatch(d -> d.statut().estEnRetard()));
    }

    /**
     * Garant : pas encore saisi dans le parcours client, compté absent. Historique : nombre de
     * dossiers SOLDEE (un dossier n'est soldé qu'une fois toutes ses échéances payées).
     */
    private static DossierScore dossierScore(Emploi emploi, Montant mensualitesEnCours, Echeancier plan, Prix prix,
            Montant apport, List<Demande> existantes) {
        int plansSoldes = (int) existantes.stream().filter(d -> d.statut() == StatutDossier.SOLDEE).count();
        return new DossierScore(TypeContrat.valueOf(emploi.typeContrat().name()), emploi.dateDebut(),
                emploi.dateFin(), plan.derniere().date(), ModeRemboursement.valueOf(emploi.modeRemboursement().name()),
                emploi.salaireNet(), mensualitesEnCours, plan.mensualiteMaximale(), prix.prixVente(),
                prix.apportMinimum(), apport, false, plansSoldes);
    }
}
