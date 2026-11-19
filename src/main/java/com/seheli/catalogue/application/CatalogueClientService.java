package com.seheli.catalogue.application;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.catalogue.domain.CategorieProduit;
import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.catalogue.domain.FournisseurRepository;
import com.seheli.catalogue.domain.Produit;
import com.seheli.catalogue.domain.ProduitRepository;
import com.seheli.contrat.application.CalculPrixService;
import com.seheli.contrat.application.SimulationService;
import com.seheli.contrat.domain.Prix;
import com.seheli.contrat.domain.Simulation;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;

/**
 * Catalogue côté client : produits proposés à l'achat à crédit, avec leur prix de vente Seheli.
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
        return proposes(categorie, "");
    }

    /**
     * Produits proposés, triés par désignation, filtrés par catégorie (toutes si vide) et par une
     * recherche libre sur la désignation et la marque. La recherche ignore la casse et les accents
     * (« refrigerateur » trouve « Réfrigérateur ») ; chaque mot doit apparaître. Vide : pas de filtre.
     */
    @Transactional(readOnly = true)
    public List<ProduitPropose> proposes(Optional<CategorieProduit> categorie, String recherche) {
        Objects.requireNonNull(categorie, "categorie");
        List<String> mots = mots(recherche == null ? "" : recherche);
        List<Produit> candidats = categorie.map(produits::disponibles).orElseGet(produits::disponibles);
        return candidats.stream()
                .filter(produit -> correspond(produit, mots))
                .map(this::proposer).flatMap(Optional::stream).toList();
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

    /**
     * Conditions d'un produit proposé pour une demande : fournisseur et prix complet ; vide si le
     * produit n'est pas proposé au client.
     */
    @Transactional(readOnly = true)
    public Optional<ConditionsProduit> conditions(UUID id) {
        Objects.requireNonNull(id, "id");
        return produits.parId(id).filter(Produit::estDisponible).flatMap(produit -> prix(produit)
                .map(prix -> new ConditionsProduit(produit.id(), produit.fournisseurId(), produit.designation(),
                        prix)));
    }

    /**
     * La mensualité « à partir de » est celle que calcule le module contrat pour la durée maximale
     * avec l'apport minimum : la plus basse proposée, identique à la simulation de la page produit.
     */
    private Optional<ProduitPropose> proposer(Produit produit) {
        return prix(produit).flatMap(prix -> {
            int dureeMin = parametres.entier(CleParametre.DUREE_MIN_MOIS);
            int dureeMax = parametres.entier(CleParametre.DUREE_MAX_MOIS);
            Simulation laPlusLongue;
            try {
                laPlusLongue = simulation.simuler(prix, dureeMax);
            } catch (IllegalArgumentException e) {
                LOG.warn("Produit {} non proposé : simulation impossible ({})", produit.id(), e.getMessage());
                return Optional.empty();
            }
            return Optional.of(new ProduitPropose(produit.id(), produit.designation(), produit.marque(),
                    produit.categorie(), prix.prixVente(), prix.apportMinimum(), prix.resteAFinancer(),
                    dureeMin, dureeMax, laPlusLongue.mensualite()));
        });
    }

    private static boolean correspond(Produit produit, List<String> mots) {
        String texte = normaliser(produit.designation() + " " + produit.marque());
        return mots.stream().allMatch(texte::contains);
    }

    private static List<String> mots(String recherche) {
        String normalisee = normaliser(recherche).strip();
        return normalisee.isEmpty() ? List.of() : List.of(normalisee.split("\\s+"));
    }

    private static String normaliser(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
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
