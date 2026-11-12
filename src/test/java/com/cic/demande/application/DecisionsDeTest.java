package com.cic.demande.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import com.cic.audit.application.JournalAuditService;
import com.cic.commun.domain.Montant;
import com.cic.contrat.application.AutorisationsEnMemoire;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.application.ContratsEnMemoire;
import com.cic.identite.application.ClientsEnMemoire;
import com.cic.identite.application.EmploiClientService;
import com.cic.identite.application.EmploisEnMemoire;
import com.cic.identite.domain.Emploi;
import com.cic.identite.domain.ModeRemboursement;
import com.cic.identite.domain.TypeContrat;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

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
