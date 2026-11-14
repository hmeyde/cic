package com.seheli.demande.application;

import java.util.Objects;

import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.PrealablesCommande;

/**
 * Dossier APPORT_ET_AUTORISATION en attente de la validation du paiement fournisseur, avec ce que
 * portera le bon de commande.
 *
 * @param analyste personne qui a décidé du dossier : elle ne peut pas valider ce paiement
 */
public record DossierACommander(Demande demande, String numeroContrat, String nomFournisseur, String analyste,
        PrealablesCommande prealables) {

    public DossierACommander {
        Objects.requireNonNull(demande, "demande");
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        Objects.requireNonNull(nomFournisseur, "nomFournisseur");
        Objects.requireNonNull(analyste, "analyste");
        Objects.requireNonNull(prealables, "prealables");
    }

    /** La personne connectée peut valider le paiement fournisseur de ce dossier. */
    public boolean validablePar(String identifiant) {
        return prealables.sontReunis() && !analyste.equals(identifiant);
    }
}
