package com.seheli.catalogue.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Fournisseur auprès duquel Seheli achète les produits, avec la remise négociée sur le prix public.
 *
 * <p>La remise est exprimée en pourcentage ({@code 8} pour 8 %). Le domaine vérifie seulement
 * qu'elle est un taux valide (0 ≤ remise &lt; 100) ; les bornes métier
 * ({@code remise.fournisseur.min.pct} – {@code remise.fournisseur.max.pct}) vivent dans la table
 * {@code parametre} et sont contrôlées par le cas d'usage, car elles peuvent changer sans
 * redéploiement.
 */
public final class Fournisseur {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    static final int NOM_LONGUEUR_MAX = 150;

    private final UUID id;
    private String nom;
    private BigDecimal remiseNegocieePct;
    private boolean actif;

    private Fournisseur(UUID id, String nom, BigDecimal remiseNegocieePct, boolean actif) {
        this.id = Objects.requireNonNull(id, "id");
        this.nom = nomValide(nom);
        this.remiseNegocieePct = remiseValide(remiseNegocieePct);
        this.actif = actif;
    }

    /** Nouveau fournisseur, actif. */
    public static Fournisseur nouveau(String nom, BigDecimal remiseNegocieePct) {
        return new Fournisseur(UUID.randomUUID(), nom, remiseNegocieePct, true);
    }

    /** Reconstitution depuis le stockage. */
    public static Fournisseur reconstituer(UUID id, String nom, BigDecimal remiseNegocieePct, boolean actif) {
        return new Fournisseur(id, nom, remiseNegocieePct, actif);
    }

    public void renommer(String nouveauNom) {
        this.nom = nomValide(nouveauNom);
    }

    /** Ne modifie que les achats futurs : le prix d'un contrat signé ne change jamais. */
    public void negocierRemise(BigDecimal nouvelleRemisePct) {
        this.remiseNegocieePct = remiseValide(nouvelleRemisePct);
    }

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    public UUID id() {
        return id;
    }

    public String nom() {
        return nom;
    }

    public BigDecimal remiseNegocieePct() {
        return remiseNegocieePct;
    }

    public boolean estActif() {
        return actif;
    }

    private static String nomValide(String nom) {
        Objects.requireNonNull(nom, "nom");
        String n = nom.strip();
        if (n.isEmpty() || n.length() > NOM_LONGUEUR_MAX) {
            throw new IllegalArgumentException("Nom de fournisseur vide ou trop long (max " + NOM_LONGUEUR_MAX + ")");
        }
        return n;
    }

    private static BigDecimal remiseValide(BigDecimal remise) {
        Objects.requireNonNull(remise, "remiseNegocieePct");
        if (remise.signum() < 0 || remise.compareTo(CENT) >= 0) {
            throw new IllegalArgumentException("Remise fournisseur hors bornes : " + remise + " %");
        }
        if (remise.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Remise fournisseur : deux décimales au plus : " + remise);
        }
        return remise;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Fournisseur f && id.equals(f.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Fournisseur[" + id + ", " + nom + ", remise " + remiseNegocieePct.toPlainString() + " %]";
    }
}
