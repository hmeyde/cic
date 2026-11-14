package com.seheli.demonstration.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.seheli.catalogue.application.ProduitService;
import com.seheli.catalogue.domain.Produit;
import com.seheli.commun.domain.Montant;
import com.seheli.comptabilite.application.ApportCapitalService;
import com.seheli.comptabilite.application.SaisieApportCapital;
import com.seheli.contrat.application.AutorisationPdfService;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.SuiviAutorisationService;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Echeance;
import com.seheli.demande.application.CommandeFournisseurService;
import com.seheli.demande.application.DecisionService;
import com.seheli.demande.application.DemandeService;
import com.seheli.demande.application.RemiseProduitService;
import com.seheli.demande.application.SuiviRemboursementService;
import com.seheli.demande.domain.Demande;
import com.seheli.demonstration.domain.GenerateurJustificatif;
import com.seheli.identite.application.ClientService;
import com.seheli.identite.application.CreationCompteClientService;
import com.seheli.identite.application.DocumentService;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.application.NouveauCompteClient;
import com.seheli.identite.application.SaisieEmploi;
import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeDocument;
import com.seheli.inscription.application.InscriptionService;
import com.seheli.inscription.application.SaisieInscription;
import com.seheli.paiement.application.PaiementService;
import com.seheli.paiement.application.SaisiePaiement;
import com.seheli.paiement.domain.ReferenceApport;
import com.seheli.paiement.domain.ReferenceEcheance;
import com.seheli.recouvrement.application.SuiviRecouvrementService;

/**
 * Gestes des clients fictifs et de l'équipe Seheli, joués par les vrais cas d'usage des modules, comme depuis
 * les écrans : chaque geste passe par les mêmes règles, laisse les mêmes traces (journal d'audit, écritures
 * comptables, événements) et prend la date de l'horloge simulée.
 *
 * <p>Les gestes du back-office sont signés par les comptes de démonstration : l'analyste décide et suit
 * les autorisations, l'administrateur valide le paiement fournisseur (séparation des tâches) et saisit les
 * apports en capital, le recouvrement enregistre les paiements reçus et suit les remboursements.
 */
@Component
@Profile("demo")
class ParcoursDemonstration {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CreationCompteClientService comptes;
    private final ClientService clients;
    private final EmploiClientService emplois;
    private final DocumentService documents;
    private final ProduitService produits;
    private final DemandeService demandes;
    private final DecisionService decisions;
    private final ContratService contrats;
    private final SuiviAutorisationService autorisations;
    private final AutorisationPdfService autorisationsPdf;
    private final PaiementService paiements;
    private final CommandeFournisseurService commandes;
    private final RemiseProduitService remises;
    private final SuiviRemboursementService remboursements;
    private final SuiviRecouvrementService recouvrement;
    private final ApportCapitalService capital;
    private final InscriptionService inscriptions;
    private final GenerateurJustificatif justificatifs;
    private final String analyste;
    private final String admin;
    private final String agentRecouvrement;

    ParcoursDemonstration(CreationCompteClientService comptes, ClientService clients, EmploiClientService emplois,
            DocumentService documents, ProduitService produits, DemandeService demandes, DecisionService decisions,
            ContratService contrats, SuiviAutorisationService autorisations, AutorisationPdfService autorisationsPdf,
            PaiementService paiements, CommandeFournisseurService commandes, RemiseProduitService remises,
            SuiviRemboursementService remboursements, SuiviRecouvrementService recouvrement,
            ApportCapitalService capital, InscriptionService inscriptions, GenerateurJustificatif justificatifs,
            @Value("${seheli.back-office.analyste-dev.identifiant}") String analyste,
            @Value("${seheli.back-office.admin-dev.identifiant}") String admin,
            @Value("${seheli.back-office.recouvrement-dev.identifiant}") String agentRecouvrement) {
        this.comptes = comptes;
        this.clients = clients;
        this.emplois = emplois;
        this.documents = documents;
        this.produits = produits;
        this.demandes = demandes;
        this.decisions = decisions;
        this.contrats = contrats;
        this.autorisations = autorisations;
        this.autorisationsPdf = autorisationsPdf;
        this.paiements = paiements;
        this.commandes = commandes;
        this.remises = remises;
        this.remboursements = remboursements;
        this.recouvrement = recouvrement;
        this.capital = capital;
        this.inscriptions = inscriptions;
        this.justificatifs = justificatifs;
        this.analyste = analyste;
        this.admin = admin;
        this.agentRecouvrement = agentRecouvrement;
    }

    /** Vrai si ce client fictif existe déjà : les données de démonstration sont chargées. */
    boolean clientExiste(ProfilClient profil) {
        return clients.parTelephone(profil.telephone()).isPresent();
    }

    // --- Client --------------------------------------------------------------------------------------

    /** Le client crée son compte (consentement), déclare son emploi et envoie ses justificatifs. */
    void ouvrirCompte(DossierDemo d) {
        ProfilClient p = d.profil();
        UUID clientId = comptes.creer(new NouveauCompteClient(p.telephone(), p.nni(), p.nom(), p.prenom(),
                p.dateNaissance(), true)).id();
        d.client(clientId);
        boolean bmci = p.modeRemboursement() == ModeRemboursement.DOMICILIATION_BMCI;
        emplois.enregistrer(clientId, new SaisieEmploi(p.employeur(), p.typeContrat(), p.dateDebut(), p.dateFin(),
                Montant.de(p.salaireNet()), p.jourPaie(), true, bmci, p.modeRemboursement()));
        envoyerJustificatif(d, TypeDocument.PIECE_IDENTITE);
        envoyerJustificatif(d, TypeDocument.CONTRAT_OU_ATTESTATION);
        envoyerJustificatif(d, TypeDocument.BULLETIN_PAIE);
        if (p.modeRemboursement() != ModeRemboursement.RETENUE_EMPLOYEUR) {
            envoyerJustificatif(d, TypeDocument.RIB);
        }
    }

    void envoyerJustificatif(DossierDemo d, TypeDocument type) {
        ProfilClient p = d.profil();
        String nom = p.prenom() + " " + p.nom();
        byte[] image = switch (type) {
            case PIECE_IDENTITE -> justificatifs.png("Carte nationale d'identité", nom,
                    "NNI " + p.nni(), "Née ou né le " + JOUR.format(p.dateNaissance()));
            case CONTRAT_OU_ATTESTATION -> justificatifs.png("Attestation de travail", nom,
                    p.employeur(), p.typeContrat() + " depuis le " + JOUR.format(p.dateDebut()));
            case BULLETIN_PAIE -> justificatifs.png("Bulletin de paie", nom, p.employeur(),
                    "Salaire net : " + p.salaireNet() + " MRU", "Versé le " + p.jourPaie() + " du mois");
            case RIB -> justificatifs.png("Relevé d'identité bancaire", nom,
                    p.modeRemboursement() == ModeRemboursement.DOMICILIATION_BMCI ? "BMCI" : "Banque du client",
                    "Compte n° 00020 " + p.nni().substring(4) + " 00");
        };
        documents.envoyer(d.clientId(), type, image);
    }

    /** Le back-office contrôle le NNI et la pièce d'identité du client. */
    void verifierIdentite(DossierDemo d) {
        clients.verifierIdentite(d.clientId());
    }

    /** Le client soumet sa demande ; l'apport est le minimum si {@code apport} est nul. */
    void soumettre(DossierDemo d, String referenceProduit, int nombreMois, Long apport) {
        UUID produitId = produits.lister().stream().filter(p -> p.reference().equals(referenceProduit))
                .map(Produit::id).findFirst()
                .orElseThrow(() -> new IllegalStateException("Produit de démonstration absent : " + referenceProduit));
        Demande demande = demandes.soumettre(d.clientId(), produitId, nombreMois,
                Optional.ofNullable(apport).map(Montant::de));
        d.demande(demande.id());
    }

    /** Le client confirme avoir reçu son produit : le remboursement démarre. */
    void confirmerRemise(DossierDemo d) {
        remises.confirmer(d.clientId(), true);
    }

    // --- Analyste ------------------------------------------------------------------------------------

    void accepter(DossierDemo d) {
        decisions.accepter(d.demandeId(), analyste);
    }

    void refuser(DossierDemo d, String motif) {
        decisions.refuser(d.demandeId(), analyste, motif);
    }

    void demanderJustificatif(DossierDemo d, TypeDocument type, String precision) {
        decisions.demanderJustificatif(d.demandeId(), analyste, type, precision);
    }

    /** L'autorisation de prélèvement, imprimée et signée par le client, est déposée : A_SIGNER → SIGNEE. */
    void deposerAutorisationSignee(DossierDemo d) {
        byte[] pdf = autorisationsPdf.pourDemande(d.demandeId()).orElseThrow().contenu();
        autorisations.deposerDocumentSigne(autorisation(d), analyste, pdf);
    }

    void transmettreAutorisation(DossierDemo d) {
        autorisations.transmettre(autorisation(d), analyste);
    }

    /** La banque vise l'ordre : le dossier passe APPORT_ET_AUTORISATION si l'apport est déjà encaissé. */
    void accepterAutorisation(DossierDemo d) {
        autorisations.accepter(autorisation(d), analyste);
    }

    /** Premier prélèvement constaté : ACCEPTEE → ACTIVE. */
    void activerAutorisation(DossierDemo d) {
        autorisations.activer(autorisation(d), analyste);
    }

    private UUID autorisation(DossierDemo d) {
        return autorisations.parDemande(d.demandeId()).orElseThrow().id();
    }

    // --- Administrateur ------------------------------------------------------------------------------

    /** Paiement fournisseur validé par une autre personne que l'analyste qui a décidé : COMMANDEE. */
    void commander(DossierDemo d) {
        commandes.commander(d.demandeId(), admin);
    }

    void apportCapital(String associe, long montant, LocalDate verseLe) {
        capital.saisir(new SaisieApportCapital(associe, String.valueOf(montant), verseLe.toString()), admin);
    }

    // --- Recouvrement --------------------------------------------------------------------------------

    /** Apport payé par Masrvi avec la référence du dossier, saisi au back-office. */
    void payerApport(DossierDemo d, LocalDate recuLe) {
        Contrat c = contrat(d);
        paiements.saisir(new SaisiePaiement(ReferenceApport.pourContrat(c.numero()).valeur(),
                c.apport().valeur().toPlainString(), recuLe.toString()), agentRecouvrement);
    }

    /** Prélèvement de l'échéance reçu, importé du relevé bancaire. */
    void payerEcheance(DossierDemo d, int rang, LocalDate recuLe) {
        Contrat c = contrat(d);
        Echeance e = c.echeance(rang).orElseThrow();
        paiements.importer(new SaisiePaiement(ReferenceEcheance.pourEcheance(c.numero(), rang).valeur(),
                e.montant().valeur().toPlainString(), recuLe.toString()), agentRecouvrement);
    }

    /** Paiement reçu avec une référence qui ne désigne aucun dossier : reste à identifier. */
    void paiementSansReference(String reference, long montant, LocalDate recuLe) {
        paiements.saisir(new SaisiePaiement(reference, String.valueOf(montant), recuLe.toString()), agentRecouvrement);
    }

    void regulariser(DossierDemo d) {
        remboursements.regulariser(d.demandeId(), agentRecouvrement);
    }

    void passerEnRecouvrement(DossierDemo d, String motif) {
        remboursements.passerEnRecouvrement(d.demandeId(), agentRecouvrement, motif);
    }

    void solder(DossierDemo d) {
        remboursements.solder(d.demandeId(), agentRecouvrement);
    }

    void noter(DossierDemo d, String texte) {
        recouvrement.noter(d.demandeId(), agentRecouvrement, texte);
    }

    Contrat contrat(DossierDemo d) {
        return contrats.parDemande(d.demandeId())
                .orElseThrow(() -> new IllegalStateException("Contrat absent pour " + d));
    }

    // --- Public --------------------------------------------------------------------------------------

    void preInscription(String nom, String telephone, String employeur, String typeContrat, String tranche,
            String produit) {
        inscriptions.inscrire(new SaisieInscription(nom, telephone, employeur, typeContrat, tranche, produit, true));
    }
}
