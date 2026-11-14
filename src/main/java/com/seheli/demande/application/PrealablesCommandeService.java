package com.seheli.demande.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.domain.Contrat;
import com.seheli.demande.domain.PrealablesCommande;
import com.seheli.paiement.application.PaiementService;

/**
 * Établit les préalables d'une commande fournisseur à partir des autres modules, par leur API
 * publique : apport encaissé (module paiement : total reçu par la référence d'apport au moins égal à
 * l'apport fixé au contrat) et autorisation de prélèvement ACCEPTEE ou ACTIVE (module contrat).
 * Sans contrat, aucun préalable n'est réuni.
 */
@Service
public class PrealablesCommandeService {

    private final ContratService contrats;
    private final PaiementService paiements;

    public PrealablesCommandeService(ContratService contrats, PaiementService paiements) {
        this.contrats = contrats;
        this.paiements = paiements;
    }

    @Transactional(readOnly = true)
    public PrealablesCommande pour(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return contrats.parDemande(demandeId).map(this::pour).orElse(PrealablesCommande.AUCUN);
    }

    private PrealablesCommande pour(Contrat contrat) {
        Montant recu = paiements.totalRecuPourDossier(contrat.demandeId());
        boolean apportEncaisse = recu.compareTo(contrat.apport()) >= 0;
        boolean autorisationAcceptee = contrats.autorisation(contrat.id())
                .map(a -> a.statut().estAcceptee()).orElse(false);
        return new PrealablesCommande(apportEncaisse, autorisationAcceptee);
    }
}
