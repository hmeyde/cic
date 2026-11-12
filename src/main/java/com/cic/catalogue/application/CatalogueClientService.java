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
import com.cic.contrat.application.SimulationService;
import com.cic.contrat.domain.Prix;
import com.cic.contrat.domain.Simulation;
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
    private final SimulationService simulation;
    private final ParametreService parametres;

    public CatalogueClientService(ProduitRepository produits, FournisseurRepository fournisseurs,
            CalculPrixService calculPrix, SimulationService simulation, ParametreService parametres) {
        this.produits = produits;
        this.fournisseurs = fournisseurs;
        this.calculPrix = calculPrix;
        this.simulation = simulation;
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

    /**
     * Simulation du produit sur {@code nombreMois} mensualités avec l'apport minimum, calculée par
     * le module contrat ; vide si le produit n'est pas proposé au client.
     *
     * @throws IllegalArgumentException si la durée sort des bornes en vigueur
     */
    @Transactional(readOnly = true)
    public Optional<Simulation> simuler(UUID id, int nombreMois) {
        Objects.requireNonNull(id, "id");
        return produits.parId(id).filter(Produit::estDisponible).flatMap(this::prix)
                .map(prix -> simulation.simuler(prix, nombreMois));
    }

    private Optional<ProduitPropose> proposer(Produit produit) {
        return prix(produit).map(prix -> {
            int dureeMin = parametres.entier(CleParametre.DUREE_MIN_MOIS);
            int dureeMax = parametres.entier(CleParametre.DUREE_MAX_MOIS);
            var reste = prix.resteAFinancer();
            return new ProduitPropose(produit.id(), produit.designation(), produit.marque(),
                    produit.categorie(), prix.prixVente(), prix.apportMinimum(), reste, dureeMin, dureeMax,
                    reste.repartir(dureeMax).getFirst());
        });
    }

    /** Prix du produit si son fournisseur est actif et si sa remise est dans les bornes en vigueur. */
    private Optional<Prix> prix(Produit produit) {
        Optional<Fournisseur> fournisseur = fournisseurs.parId(produit.fournisseurId()).filter(Fournisseur::estActif);
        if (fournisseur.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(calculPrix.calculer(produit.prixPublic(), fournisseur.get().remiseNegocieePct()));
        } catch (IllegalArgumentException e) {
            LOG.warn("Produit {} non proposé : prix incalculable ({})", produit.id(), e.getMessage());
            return Optional.empty();
        }
    }
}
