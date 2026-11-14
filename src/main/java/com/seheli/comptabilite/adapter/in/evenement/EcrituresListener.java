package com.seheli.comptabilite.adapter.in.evenement;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.seheli.comptabilite.application.ComptabiliteService;
import com.seheli.demande.CommandeFournisseurValidee;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.paiement.PaiementNonRapproche;
import com.seheli.paiement.PaiementRapproche;

/**
 * Écoute les mouvements d'argent des modules paiement et demande et en passe les écritures : apport
 * ou échéance encaissé, paiement à identifier, paiement fournisseur ; et la vente quand le produit est
 * remis au client (dossier LIVREE).
 *
 * <p>Écouteurs de module Spring Modulith : appelés après la validation de la transaction qui publie
 * l'événement, dans leur propre transaction ; la publication est gardée dans le registre
 * d'événements (outbox) jusqu'à ce que l'écriture soit passée. Rejouer un événement ne crée pas de
 * doublon : l'écriture d'un même fait n'est passée qu'une fois.
 */
@Component
class EcrituresListener {

    private final ComptabiliteService comptabilite;

    EcrituresListener(ComptabiliteService comptabilite) {
        this.comptabilite = comptabilite;
    }

    @ApplicationModuleListener
    void paiementRapproche(PaiementRapproche e) {
        switch (e.objet()) {
            case APPORT -> comptabilite.apportEncaisse(e.paiementId(), e.demandeId(), e.contratId(),
                    e.numeroContrat(), e.montant(), e.recuLe());
            case ECHEANCE -> comptabilite.echeanceEncaissee(e.paiementId(), e.demandeId(), e.contratId(),
                    e.numeroContrat(), e.rangEcheance(), e.montant(), e.recuLe());
        }
    }

    @ApplicationModuleListener
    void paiementNonRapproche(PaiementNonRapproche e) {
        comptabilite.paiementAIdentifier(e.paiementId(), e.montant(), e.recuLe());
    }

    @ApplicationModuleListener
    void commandeFournisseurValidee(CommandeFournisseurValidee e) {
        comptabilite.paiementFournisseur(e.commandeId(), e.numero(), e.demandeId(), e.contratId(), e.montant(),
                e.le());
    }

    @ApplicationModuleListener
    void statutDossierChange(StatutDossierChange e) {
        if (e.vers() == StatutDossier.LIVREE) {
            comptabilite.venteLivree(e.demandeId(), e.le());
        }
    }
}
