package com.cic.demande.application;

import java.util.Objects;

import com.cic.demande.domain.CommandeFournisseur;

/** Commande fournisseur listée au back-office, avec le nom du fournisseur. */
public record LigneCommande(CommandeFournisseur commande, String nomFournisseur) {

    public LigneCommande {
        Objects.requireNonNull(commande, "commande");
        Objects.requireNonNull(nomFournisseur, "nomFournisseur");
    }
}
