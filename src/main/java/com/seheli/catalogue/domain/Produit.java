package com.seheli.catalogue.domain;

import java.util.Objects;
import java.util.UUID;

import com.seheli.commun.domain.Montant;

/**
 * Produit du catalogue : téléphone ou électroménager vendu à crédit.
 *
 * <p>Le prix public est celui du marché ; le prix d'achat (remise fournisseur) et le prix de
 * vente (marge Seheli) en sont dérivés au moment de la simulation et figés à la signature.
 * Un produit indisponible reste en base (dossiers existants) mais n'est plus proposé.
 */
public final class Produit {

    static final int REFERENCE_LONGUEUR_MAX = 50;
    static final int DESIGNATION_LONGUEUR_MAX = 200;
    static final int MARQUE_LONGUEUR_MAX = 100;

    private final UUID id;
    private final String reference;
    private String designation;
    private String marque;
    private CategorieProduit categorie;
    private UUID fournisseurId;
    private Montant prixPublic;
    private boolean disponible;

    private Produit(UUID id, String reference, String designation, String marque, CategorieProduit categorie,
            UUID fournisseurId, Montant prixPublic, boolean disponible) {
        this.id = Objects.requireNonNull(id, "id");
        this.reference = texte(reference, "reference", REFERENCE_LONGUEUR_MAX);
        this.designation = texte(designation, "designation", DESIGNATION_LONGUEUR_MAX);
        this.marque = texte(marque, "marque", MARQUE_LONGUEUR_MAX);
        this.categorie = Objects.requireNonNull(categorie, "categorie");
        this.fournisseurId = Objects.requireNonNull(fournisseurId, "fournisseurId");
        this.prixPublic = prixValide(prixPublic);
        this.disponible = disponible;
    }

    /** Nouveau produit, disponible. */
    public static Produit nouveau(String reference, String designation, String marque, CategorieProduit categorie,
            UUID fournisseurId, Montant prixPublic) {
        return new Produit(UUID.randomUUID(), reference, designation, marque, categorie, fournisseurId, prixPublic,
                true);
    }

    /** Reconstitution depuis le stockage. */
    public static Produit reconstituer(UUID id, String reference, String designation, String marque,
            CategorieProduit categorie, UUID fournisseurId, Montant prixPublic, boolean disponible) {
        return new Produit(id, reference, designation, marque, categorie, fournisseurId, prixPublic, disponible);
    }

    public void decrire(String nouvelleDesignation, String nouvelleMarque, CategorieProduit nouvelleCategorie) {
        this.designation = texte(nouvelleDesignation, "designation", DESIGNATION_LONGUEUR_MAX);
        this.marque = texte(nouvelleMarque, "marque", MARQUE_LONGUEUR_MAX);
        this.categorie = Objects.requireNonNull(nouvelleCategorie, "categorie");
    }

    /** Ne modifie que les simulations futures : le prix d'un contrat signé ne change jamais. */
    public void changerPrixPublic(Montant nouveauPrix) {
        this.prixPublic = prixValide(nouveauPrix);
    }

    public void changerFournisseur(UUID nouveauFournisseurId) {
        this.fournisseurId = Objects.requireNonNull(nouveauFournisseurId, "fournisseurId");
    }

    public void rendreDisponible() {
        this.disponible = true;
    }

    public void rendreIndisponible() {
        this.disponible = false;
    }

    public UUID id() {
        return id;
    }

    public String reference() {
        return reference;
    }

    public String designation() {
        return designation;
    }

    public String marque() {
        return marque;
    }

    public CategorieProduit categorie() {
        return categorie;
    }

    public UUID fournisseurId() {
        return fournisseurId;
    }

    public Montant prixPublic() {
        return prixPublic;
    }

    public boolean estDisponible() {
        return disponible;
    }

    private static String texte(String valeur, String champ, int longueurMax) {
        Objects.requireNonNull(valeur, champ);
        String v = valeur.strip();
        if (v.isEmpty() || v.length() > longueurMax) {
            throw new IllegalArgumentException("Produit : " + champ + " vide ou trop long (max " + longueurMax + ")");
        }
        return v;
    }

    private static Montant prixValide(Montant prix) {
        Objects.requireNonNull(prix, "prixPublic");
        if (prix.estZero()) {
            throw new IllegalArgumentException("Le prix public d'un produit doit être positif");
        }
        return prix;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Produit p && id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Produit[" + id + ", " + reference + ", " + prixPublic + (disponible ? "" : ", indisponible") + "]";
    }
}
