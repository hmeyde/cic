package com.cic.demande.application;

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

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.domain.Acteur;
import com.cic.catalogue.application.FournisseurService;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.domain.Contrat;
import com.cic.demande.CommandeFournisseurValidee;
import com.cic.demande.StatutDossierChange;
import com.cic.demande.domain.ChangementStatut;
import com.cic.demande.domain.CommandeFournisseur;
import com.cic.demande.domain.CommandeFournisseurRepository;
import com.cic.demande.domain.ContenuBonCommande;
import com.cic.demande.domain.Decision;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandeRepository;
import com.cic.demande.domain.PrealablesCommande;
import com.cic.demande.domain.RenduBonCommandePdf;
import com.cic.demande.domain.StatutDossier;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;

/**
 * Commande fournisseur : validation du paiement fournisseur d'un dossier APPORT_ET_AUTORISATION et
 * émission de son bon de commande, qui fait passer le dossier à COMMANDEE.
 *
 * <p>Règles, appliquées par le domaine : le fournisseur n'est payé qu'une fois l'apport encaissé ET
 * l'autorisation de prélèvement ACCEPTEE (vérifiés de nouveau au moment de la validation) ; la
 * personne qui a décidé du dossier ne valide pas son paiement fournisseur.
 *
 * <p>Dans une seule transaction : dossier verrouillé (deux validations simultanées ne commandent
 * pas deux fois), transition, commande enregistrée (au plus une par dossier, contrainte en base),
 * entrée au journal d'audit, événements {@link StatutDossierChange} et
 * {@link CommandeFournisseurValidee} publiés.
 */
@Service
public class CommandeFournisseurService {

    private static final Logger LOG = LoggerFactory.getLogger(CommandeFournisseurService.class);

    private final DemandeRepository demandes;
    private final CommandeFournisseurRepository commandes;
    private final PrealablesCommandeService prealables;
    private final ContratService contrats;
    private final FournisseurService fournisseurs;
    private final ParametreService parametres;
    private final RenduBonCommandePdf rendu;
    private final JournalAuditService journal;
    private final ApplicationEventPublisher evenements;
    private final Clock clock;

    public CommandeFournisseurService(DemandeRepository demandes, CommandeFournisseurRepository commandes,
            PrealablesCommandeService prealables, ContratService contrats, FournisseurService fournisseurs,
            ParametreService parametres, RenduBonCommandePdf rendu, JournalAuditService journal,
            ApplicationEventPublisher evenements, Clock clock) {
        this.demandes = demandes;
        this.commandes = commandes;
        this.prealables = prealables;
        this.contrats = contrats;
        this.fournisseurs = fournisseurs;
        this.parametres = parametres;
        this.rendu = rendu;
        this.journal = journal;
        this.evenements = evenements;
        this.clock = clock;
    }

    /** Dossiers APPORT_ET_AUTORISATION à commander, du plus ancien au plus récent. */
    @Transactional(readOnly = true)
    public List<DossierACommander> aCommander() {
        return demandes.parStatutDeLaPlusAncienne(StatutDossier.APPORT_ET_AUTORISATION).stream()
                .map(d -> new DossierACommander(d, contrat(d).numero(), nomFournisseur(d.termes().fournisseurId()),
                        d.decision().map(Decision::analyste).orElse(""), prealables.pour(d.id())))
                .toList();
    }

    /** Commandes les plus récemment validées d'abord. */
    @Transactional(readOnly = true)
    public List<LigneCommande> dernieres(int limite) {
        return commandes.dernieres(limite).stream()
                .map(c -> new LigneCommande(c, nomFournisseur(c.fournisseurId()))).toList();
    }

    /**
     * Valide le paiement fournisseur et émet le bon de commande : APPORT_ET_AUTORISATION → COMMANDEE.
     *
     * @param validateur identifiant de connexion du back-office
     * @throws DossierIntrouvableException dossier inconnu
     * @throws com.cic.demande.domain.TransitionInterditeException dossier pas APPORT_ET_AUTORISATION
     * @throws com.cic.demande.domain.PrealablesManquantsException apport non encaissé ou autorisation
     *         non acceptée
     * @throws com.cic.demande.domain.SeparationDesTachesException le validateur a décidé du dossier
     */
    @Transactional
    public CommandeFournisseur commander(UUID demandeId, String validateur) {
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(validateur, "validateur");
        Demande demande = demandes.parIdVerrouillee(demandeId).orElseThrow(DossierIntrouvableException::new);
        Instant maintenant = clock.instant();
        ChangementStatut changement = demande.commander(validateur, prealables.pour(demandeId), maintenant);
        Contrat contrat = contrat(demande);
        CommandeFournisseur commande = CommandeFournisseur.emettre(
                numero(LocalDate.ofInstant(maintenant, clock.getZone()), commandes.prochainNumeroOrdre()), demande,
                contrat.id(), contrat.numero(), validateur, maintenant);
        demandes.enregistrer(demande);
        commandes.enregistrer(commande);
        journal.journaliser(JournalDossier.transition(demande.id(), maintenant, Acteur.backOffice(validateur),
                ActionDossier.COMMANDE_FOURNISSEUR, changement.depuis(), changement.vers(),
                "Bon de commande " + commande.numero() + " — paiement fournisseur de "
                        + commande.montant().valeur().toPlainString() + " MRU validé"));
        evenements.publishEvent(new StatutDossierChange(demande.id(), demande.clientId(), changement.depuis(),
                changement.vers(), maintenant));
        evenements.publishEvent(new CommandeFournisseurValidee(commande.id(), commande.numero(), demande.id(),
                contrat.id(), commande.fournisseurId(), commande.montant(), maintenant));
        LOG.info("Commande fournisseur {} validée : {}", commande.numero(), demande);
        return commande;
    }

    /** Bon de commande du dossier en PDF ; vide tant que le paiement fournisseur n'est pas validé. */
    @Transactional(readOnly = true)
    public Optional<BonCommandePdf> bonDeCommande(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return commandes.parDemande(demandeId).map(c -> {
            Demande demande = demandes.parId(c.demandeId())
                    .orElseThrow(() -> new IllegalStateException("Dossier introuvable pour " + c.numero()));
            byte[] pdf = rendu.rendre(new ContenuBonCommande(c, nomFournisseur(c.fournisseurId()),
                    demande.termes().prixPublic(), LocalDate.ofInstant(c.valideLe(), clock.getZone()),
                    parametres.texte(CleParametre.SOCIETE_RAISON_SOCIALE)));
            return new BonCommandePdf("bon-de-commande-" + c.numero() + ".pdf", pdf);
        });
    }

    /** Numéro lisible du bon de commande, par exemple {@code BC-2026-000042}. */
    static String numero(LocalDate le, long ordre) {
        return "BC-%d-%06d".formatted(le.getYear(), ordre);
    }

    private Contrat contrat(Demande d) {
        return contrats.parDemande(d.id())
                .orElseThrow(() -> new IllegalStateException("Contrat introuvable pour " + d));
    }

    private String nomFournisseur(UUID fournisseurId) {
        return fournisseurs.parId(fournisseurId).nom();
    }
}
