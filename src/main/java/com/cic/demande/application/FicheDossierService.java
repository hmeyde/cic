package com.cic.demande.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.audit.application.JournalAuditService;
import com.cic.contrat.application.SuiviAutorisationService;
import com.cic.demande.domain.DemandeRepository;
import com.cic.identite.application.ClientService;
import com.cic.identite.application.DocumentService;
import com.cic.identite.application.EmploiClientService;

/**
 * Fiche dossier du back-office : tout ce dont l'analyste a besoin pour décider, sur une seule page.
 * L'autorisation de prélèvement du dossier y figure dès l'acceptation. Chaque consultation est journalisée, sans donnée personnelle.
 */
@Service
public class FicheDossierService {

    private static final Logger LOG = LoggerFactory.getLogger(FicheDossierService.class);

    private final DemandeRepository demandes;
    private final ClientService clients;
    private final EmploiClientService emplois;
    private final DocumentService documents;
    private final JournalAuditService journal;
    private final SuiviAutorisationService autorisations;

    public FicheDossierService(DemandeRepository demandes, ClientService clients, EmploiClientService emplois,
            DocumentService documents, JournalAuditService journal, SuiviAutorisationService autorisations) {
        this.journal = journal;
        this.demandes = demandes;
        this.clients = clients;
        this.emplois = emplois;
        this.documents = documents;
        this.autorisations = autorisations;
    }

    /** Fiche du dossier ; vide si la demande n'existe pas. */
    @Transactional(readOnly = true)
    public Optional<FicheDossier> fiche(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        Optional<FicheDossier> fiche = demandes.parId(demandeId).map(d -> new FicheDossier(d,
                clients.parId(d.clientId()), emplois.parClient(d.clientId()), documents.parClient(d.clientId()),
                journal.historique(JournalDossier.objet(d.id())), autorisations.parDemande(d.id())));
        fiche.ifPresent(f -> LOG.info("Fiche dossier consultée par le back-office : {}", f.demande()));
        return fiche;
    }
}
