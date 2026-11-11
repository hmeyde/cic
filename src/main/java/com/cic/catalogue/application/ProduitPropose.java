package com.cic.catalogue.application;

import java.util.Objects;
import java.util.UUID;

import com.cic.catalogue.domain.CategorieProduit;
import com.cic.commun.domain.Montant;

/**
 * Produit tel que le client le voit dans le catalogue : prix de vente CIC (prix public + marge)
 * et conditions indicatives avec l'apport minimum. Le prix d'achat et la remise fournisseur
 * ne sont jamais exposés au client.
 *
 * @param mensualiteIndicative mensualité sur la durée maximale avec l'apport minimum ;
 *                             la simulation donne le montant exact
 */
public record ProduitPropose(UUID id, String designation, String marque, CategorieProduit categorie,
        Montant prixVente, Montant apportMinimum, Montant resteAFinancer, int dureeMinMois, int dureeMaxMois,
        Montant mensualiteIndicative) {

    public ProduitPropose {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(designation, "designation");
        Objects.requireNonNull(marque, "marque");
        Objects.requireNonNull(categorie, "categorie");
        Objects.requireNonNull(prixVente, "prixVente");
        Objects.requireNonNull(apportMinimum, "apportMinimum");
        Objects.requireNonNull(resteAFinancer, "resteAFinancer");
        Objects.requireNonNull(mensualiteIndicative, "mensualiteIndicative");
    }
}
