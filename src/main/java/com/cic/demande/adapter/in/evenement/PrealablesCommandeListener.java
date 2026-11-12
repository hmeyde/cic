package com.cic.demande.adapter.in.evenement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.cic.contrat.AutorisationPrelevementStatutChange;
import com.cic.demande.application.ApportEtAutorisationService;
import com.cic.paiement.PaiementRapproche;

/**
 * Écoute les modules paiement et contrat : quand un paiement d'apport est rattaché à un dossier ou que son
 * autorisation de prélèvement est acceptée par la banque, vérifie si le dossier peut passer à
 * APPORT_ET_AUTORISATION. Les deux arrivent dans n'importe quel ordre : c'est le second qui fait
 * passer le dossier.
 *
 * <p>Écouteurs de module Spring Modulith : appelés après la validation de la transaction qui publie
 * l'événement, dans leur propre transaction ; la publication est gardée dans le registre
 * d'événements (outbox) jusqu'à ce que le traitement réussisse.
 */
@Component
class PrealablesCommandeListener {

    private static final Logger LOG = LoggerFactory.getLogger(PrealablesCommandeListener.class);

    private final ApportEtAutorisationService apportEtAutorisation;

    PrealablesCommandeListener(ApportEtAutorisationService apportEtAutorisation) {
        this.apportEtAutorisation = apportEtAutorisation;
    }

    @ApplicationModuleListener
    void paiementRapproche(PaiementRapproche evenement) {
        if (!evenement.estApport()) {
            return;
        }
        LOG.debug("Paiement {} rattaché au dossier {}", evenement.paiementId(), evenement.demandeId());
        apportEtAutorisation.constaterSiReunis(evenement.demandeId());
    }

    @ApplicationModuleListener
    void autorisationChangee(AutorisationPrelevementStatutChange evenement) {
        if (evenement.vers().estAcceptee()) {
            apportEtAutorisation.constaterSiReunis(evenement.demandeId());
        }
    }
}
