package com.seheli.demande.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.AutorisationsEnMemoire;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.ContratsEnMemoire;
import com.seheli.identite.application.ClientsEnMemoire;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.application.EmploisEnMemoire;
import com.seheli.identite.domain.Emploi;
import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeContrat;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

/** {@link DecisionService} branché sur des stockages en mémoire, avec ses emplois et ses contrats. */
public final class DecisionsDeTest {

    public final EmploisEnMemoire emplois;
    public final ContratsEnMemoire contrats = new ContratsEnMemoire();
    public final AutorisationsEnMemoire autorisations = new AutorisationsEnMemoire();
    public final ContratService contratService;
    public final DecisionService service;

    public DecisionsDeTest(DemandesEnMemoire demandes, JournalAuditService journal,
            ApplicationEventPublisher evenements, Clock clock) {
        this(demandes, journal, new EmploisEnMemoire(), evenements, clock);
    }

    public DecisionsDeTest(DemandesEnMemoire demandes, JournalAuditService journal, EmploisEnMemoire emplois,
            ApplicationEventPublisher evenements, Clock clock) {
        this.emplois = emplois;
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        contratService = new ContratService(contrats, autorisations, parametres, clock);
        service = new DecisionService(demandes, journal,
                new EmploiClientService(new ClientsEnMemoire(), emplois, clock),
                contratService, evenements, clock);
    }

    /** Emploi en CDI payé le 25 : le contrat peut être établi. */
    public void emploiCdi(UUID clientId) {
        emplois.enregistrer(Emploi.nouveau(clientId, "SNIM", TypeContrat.CDI, LocalDate.of(2020, 1, 1), null,
                Montant.de(30_000), 25, true, false, ModeRemboursement.RETENUE_EMPLOYEUR));
    }
}
