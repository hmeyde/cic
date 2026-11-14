package com.seheli.demande.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.contrat.application.AutorisationsEnMemoire;
import com.seheli.contrat.application.ContratAEtablir;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.ContratsEnMemoire;
import com.seheli.contrat.domain.AutorisationPrelevement;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.DocumentSigne;
import com.seheli.contrat.domain.Prix;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.TermesDemande;
import com.seheli.identite.domain.FormatDocument;
import com.seheli.paiement.application.PaiementService;
import com.seheli.paiement.application.PaiementsEnMemoire;
import com.seheli.paiement.application.SaisiePaiement;
import com.seheli.paiement.domain.ReferenceApport;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

/** Modules contrat et paiement en mémoire, pour établir les préalables d'une commande fournisseur. */
public final class PrealablesDeTest {

    public final ContratsEnMemoire contrats = new ContratsEnMemoire();
    public final AutorisationsEnMemoire autorisations = new AutorisationsEnMemoire();
    public final PaiementsEnMemoire paiements = new PaiementsEnMemoire();
    public final ContratService contratService;
    public final PaiementService paiementService;
    public final PrealablesCommandeService service;
    private final Clock clock;

    public PrealablesDeTest(Clock clock) {
        this.clock = clock;
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        contratService = new ContratService(contrats, autorisations, parametres, clock);
        paiementService = new PaiementService(paiements, contratService, new JournalAuditService(
                new JournalEnMemoire()), e -> { }, clock);
        service = new PrealablesCommandeService(contratService, paiementService);
    }

    /** Contrat établi pour le dossier, avec son autorisation de prélèvement A_SIGNER. */
    public Contrat contrat(Demande d) {
        TermesDemande t = d.termes();
        return contratService.etablir(new ContratAEtablir(d.id(), d.clientId(), t.produitId(), t.fournisseurId(),
                t.designationProduit(), new Prix(t.prixPublic(), t.prixAchat(), t.prixVente(), t.apportMinimum()),
                t.apport(), t.nombreMois(), 25, Optional.empty(), d.statutChangeLe()));
    }

    /** Paiement reçu par la référence d'apport du contrat. */
    public void payer(Contrat c, String reference, String montant) {
        paiementService.saisir(new SaisiePaiement(reference, montant, clock.instant().atZone(clock.getZone())
                .toLocalDate().toString()), "recouvrement");
    }

    public void payerApport(Contrat c) {
        payer(c, ReferenceApport.pourContrat(c.numero()).valeur(), c.apport().valeur().toPlainString());
    }

    /** Autorisation signée, transmise puis acceptée par la banque. */
    public void accepterAutorisation(Contrat c) {
        Instant le = clock.instant();
        AutorisationPrelevement a = autorisations.parContrat(c.id()).orElseThrow();
        a.deposerDocumentSigne(DocumentSigne.nouveau(FormatDocument.PDF, 100, le, "analyste"));
        a.transmettre(le);
        a.accepter(le);
        autorisations.mettreAJour(a);
    }

    public void revoquerAutorisation(Contrat c) {
        AutorisationPrelevement a = autorisations.parContrat(c.id()).orElseThrow();
        a.revoquer(clock.instant());
        autorisations.mettreAJour(a);
    }
}
