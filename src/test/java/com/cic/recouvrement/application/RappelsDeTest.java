package com.cic.recouvrement.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.support.ResourceBundleMessageSource;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.commun.domain.Montant;
import com.cic.contrat.application.AutorisationsEnMemoire;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.application.ContratsEnMemoire;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Echeance;
import com.cic.contrat.domain.Prix;
import com.cic.contrat.domain.StatutEcheance;
import com.cic.demande.application.DemandesEnMemoire;
import com.cic.demande.application.RetardDossierService;
import com.cic.demande.application.StatutDossierService;
import com.cic.demande.application.SuiviRemboursementService;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandesFixtures;
import com.cic.demande.domain.StatutDossier;
import com.cic.identite.application.ClientService;
import com.cic.identite.application.ClientsEnMemoire;
import com.cic.identite.domain.Client;
import com.cic.notification.domain.Message;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

/**
 * Le 12/11/2026 à Nouakchott : un client, son dossier et son contrat de l'exemple chiffré
 * (6 × 2 080 MRU) dont la première échéance tombe, par exemple, le 13/11/2026, la veille étant
 * aujourd'hui ; services de rappel et de retard branchés sur les mêmes stockages en mémoire.
 */
public final class RappelsDeTest {

    public static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    public static final Instant AUJOURDHUI = Instant.parse("2026-11-12T09:00:00Z");
    public static final LocalDate DEMAIN = LocalDate.of(2026, 11, 13);

    public final Clock clock = Clock.fixed(AUJOURDHUI, NOUAKCHOTT);
    public final ContratsEnMemoire contrats = new ContratsEnMemoire();
    public final DemandesEnMemoire demandes = new DemandesEnMemoire();
    public final ClientsEnMemoire clients = new ClientsEnMemoire();
    public final RelancesEnMemoire relances = new RelancesEnMemoire();
    public final List<Message> envoyes = new ArrayList<>();
    public final JournalEnMemoire journal = new JournalEnMemoire();
    public final List<Object> evenements = new ArrayList<>();
    public final RappelPrelevementService service;
    public final RetardEcheanceService retards;
    public final NotesEnMemoire notes = new NotesEnMemoire();
    public final SuiviRemboursementService remboursements;
    public final SuiviRecouvrementService suivi;
    private long ordre;

    public RappelsDeTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        ContratService contratService = new ContratService(contrats, new AutorisationsEnMemoire(), parametres, clock);
        service = new RappelPrelevementService(
                contratService, new StatutDossierService(demandes), new ClientService(clients), relances, envoyes::add,
                parametres, messages, clock);
        retards = new RetardEcheanceService(contratService, new StatutDossierService(demandes),
                new RetardDossierService(demandes, new JournalAuditService(journal), evenements::add, clock),
                new ClientService(clients), relances, envoyes::add, parametres, messages, clock);
        remboursements = new SuiviRemboursementService(demandes, contratService, new JournalAuditService(journal),
                evenements::add, clock);
        suivi = new SuiviRecouvrementService(contratService, new StatutDossierService(demandes), remboursements,
                new ClientService(clients), relances, notes, clock);
    }

    public Client client(String telephone) {
        Client c = Client.nouveau(String.format("99900%05d", ++ordre), "Ould Ahmed", "Mohamed",
                LocalDate.of(1990, 5, 1), telephone, AUJOURDHUI);
        clients.enregistrer(c);
        return c;
    }

    /** Dossier au statut donné et son contrat, première échéance à {@code premiere}, puis de mois en mois. */
    public Contrat contrat(Client client, StatutDossier statut, LocalDate premiere) {
        Demande d = DemandesFixtures.demande(client.id(), statut);
        demandes.enregistrer(d);
        List<Echeance> echeances = new ArrayList<>();
        for (int rang = 1; rang <= 6; rang++) {
            echeances.add(Echeance.reconstituer(UUID.randomUUID(), rang, premiere.plusMonths(rang - 1),
                    Montant.de(2_080), StatutEcheance.A_VENIR));
        }
        Contrat c = Contrat.reconstituer(UUID.randomUUID(), String.format("CIC-2026-%06d", ++ordre), d.id(),
                client.id(), UUID.randomUUID(), UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), echeances, AUJOURDHUI);
        contrats.enregistrer(c);
        return c;
    }
}
