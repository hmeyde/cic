package com.seheli.catalogue.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.catalogue.domain.FournisseurRepository;
import com.seheli.catalogue.domain.Produit;
import com.seheli.catalogue.domain.ProduitRepository;
import com.seheli.commun.domain.Montant;

/**
 * Création et modification des produits du catalogue (back-office, rôle ADMIN).
 *
 * <p>La référence est unique et ne change plus après la création (elle identifie le produit dans
 * les dossiers). Un produit est rattaché à un fournisseur actif. On ne supprime pas un produit :
 * on le rend indisponible, il n'est alors plus proposé aux clients. Un nouveau prix public ne
 * vaut que pour les simulations futures.
 */
@Service
public class ProduitService {

    private final ProduitRepository produits;
    private final FournisseurRepository fournisseurs;

    public ProduitService(ProduitRepository produits, FournisseurRepository fournisseurs) {
        this.produits = produits;
        this.fournisseurs = fournisseurs;
    }

    /** Tous les produits, triés par désignation. */
    @Transactional(readOnly = true)
    public List<Produit> lister() {
        return produits.tous();
    }

    /** @throws ProduitIntrouvableException si l'identifiant est inconnu */
    @Transactional(readOnly = true)
    public Produit parId(UUID id) {
        return produits.parId(id).orElseThrow(ProduitIntrouvableException::new);
    }

    /**
     * Crée un produit disponible.
     *
     * @throws ReferenceProduitDejaUtiliseeException si la référence est déjà prise
     * @throws FournisseurIntrouvableException si le fournisseur est inconnu
     * @throws FournisseurInactifException si le fournisseur est désactivé
     */
    @Transactional
    public Produit creer(String reference, String designation, String marque, CategorieProduit categorie,
            UUID fournisseurId, Montant prixPublic) {
        verifierFournisseurActif(fournisseurId);
        Produit produit = Produit.nouveau(reference, designation, marque, categorie, fournisseurId, prixPublic);
        if (produits.parReference(produit.reference()).isPresent()) {
            throw new ReferenceProduitDejaUtiliseeException();
        }
        produits.enregistrer(produit);
        return produit;
    }

    /**
     * Modifie la description, le fournisseur, le prix public et la disponibilité d'un produit.
     *
     * <p>Le fournisseur n'est contrôlé que s'il change : un produit dont le fournisseur a été
     * désactivé peut encore être modifié ou rendu indisponible.
     *
     * @throws ProduitIntrouvableException si l'identifiant est inconnu
     * @throws FournisseurIntrouvableException si le nouveau fournisseur est inconnu
     * @throws FournisseurInactifException si le nouveau fournisseur est désactivé
     */
    @Transactional
    public Produit modifier(UUID id, String designation, String marque, CategorieProduit categorie,
            UUID fournisseurId, Montant prixPublic, boolean disponible) {
        Objects.requireNonNull(fournisseurId, "fournisseurId");
        Produit produit = parId(id);
        if (!fournisseurId.equals(produit.fournisseurId())) {
            verifierFournisseurActif(fournisseurId);
            produit.changerFournisseur(fournisseurId);
        }
        produit.decrire(designation, marque, categorie);
        produit.changerPrixPublic(prixPublic);
        changerDisponibilite(produit, disponible);
        produits.enregistrer(produit);
        return produit;
    }

    /**
     * Rend un produit disponible ou indisponible.
     *
     * @throws ProduitIntrouvableException si l'identifiant est inconnu
     */
    @Transactional
    public Produit changerDisponibilite(UUID id, boolean disponible) {
        Produit produit = parId(id);
        changerDisponibilite(produit, disponible);
        produits.enregistrer(produit);
        return produit;
    }

    private static void changerDisponibilite(Produit produit, boolean disponible) {
        if (disponible) {
            produit.rendreDisponible();
        } else {
            produit.rendreIndisponible();
        }
    }

    private void verifierFournisseurActif(UUID fournisseurId) {
        Objects.requireNonNull(fournisseurId, "fournisseurId");
        Fournisseur fournisseur = fournisseurs.parId(fournisseurId).orElseThrow(FournisseurIntrouvableException::new);
        if (!fournisseur.estActif()) {
            throw new FournisseurInactifException();
        }
    }
}
