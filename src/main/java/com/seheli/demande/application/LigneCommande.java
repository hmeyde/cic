package com.seheli.demande.application;

import java.util.Objects;

import com.seheli.demande.domain.CommandeFournisseur;

/** Commande fournisseur listée au back-office, avec le nom du fournisseur. */
public record LigneCommande(CommandeFournisseur commande, String nomFournisseur) {

    public LigneCommande {
        Objects.requireNonNull(commande, "commande");
        Objects.requireNonNull(nomFournisseur, "nomFournisseur");
    }
}
