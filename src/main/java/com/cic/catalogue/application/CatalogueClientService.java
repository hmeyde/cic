package com.cic.catalogue.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.catalogue.domain.CategorieProduit;
import com.cic.catalogue.domain.Fournisseur;
import com.cic.catalogue.domain.FournisseurRepository;
import com.cic.catalogue.domain.Produit;
import com.cic.catalogue.domain.ProduitRepository;
import com.cic.contrat.application.CalculPrixService;
import com.cic.contrat.domain.Prix;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;

/**
 * Catalogue côté client : produits proposés à l'achat à crédit, avec leur prix de vente CIC.
 *
 * <p>Un produit est proposé s'il est disponible et si son fournisseur est actif. Le prix est
 * calculé avec les paramètres en vigueur (marge, apport minimum, durées) ; un produit dont le prix
 * ne peut pas être calculé (remise du fournisseur hors des bornes actuelles) n'est pas proposé,
 * puisqu'il ne pourrait pas être simulé.
 */
@Service
public class CatalogueClientService {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogueClientService.class);

    private final ProduitRepository produits;
    private final FournisseurRepository fournisseurs;
    private final CalculPrixService calculPrix;
    private final ParametreService parametres;

    public CatalogueClientService(ProduitRepository produits, FournisseurRepository fournisseurs,
            CalculPrixService calculPrix, ParametreService parametres) {
        this.produits = produits;
        this.fournisseurs = fournisseurs;
        this.calculPrix = calculPrix;
        this.parametres = parametres;
    }

    /** Produits proposés, triés par désignation ; tous si la catégorie est vide. */
    @Transactional(readOnly = true)
    public List<ProduitPropose> proposes(Optional<CategorieProduit> categorie) {
        Objects.requireNonNull(categorie, "categorie");
        List<Produit> candidats = categorie.map(produits::disponibles).orElseGet(produits::disponibles);
        return candidats.stream().map(this::proposer).flatMap(Optional::stream).toList();
    }

    /** Le produit s'il est proposé au client, vide sinon (inconnu, indisponible ou fournisseur inactif). */
    @Transactional(readOnly = true)
    public Optional<ProduitPropose> propose(UUID id) {
        Objects.requireNonNull(id, "id");
        return produits.parId(id).filter(Produit::estDisponible).flatMap(this::proposer);
    }

    private Optional<ProduitPropose> proposer(Produit produit) {
        Optional<Fournisseur> fournisseur = fournisseurs.parId(produit.fournisseurId()).filter(Fournisseur::estActif);
        if (fournisseur.isEmpty()) {
            return Optional.empty();
        }
        Prix prix;
        try {
            prix = calculPrix.calculer(produit.prixPublic(), fournisseur.get().remiseNegocieePct());
        } catch (IllegalArgumentException e) {
            LOG.warn("Produit {} non proposé : prix incalculable ({})", produit.id(), e.getMessage());
            return Optional.empty();
        }
        int dureeMin = parametres.entier(CleParametre.DUREE_MIN_MOIS);
        int dureeMax = parametres.entier(CleParametre.DUREE_MAX_MOIS);
        var reste = prix.resteAFinancer();
        return Optional.of(new ProduitPropose(produit.id(), produit.designation(), produit.marque(),
                produit.categorie(), prix.prixVente(), prix.apportMinimum(), reste, dureeMin, dureeMax,
                reste.repartir(dureeMax).getFirst()));
    }
}
