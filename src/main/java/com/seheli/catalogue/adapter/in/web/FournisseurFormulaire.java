package com.seheli.catalogue.adapter.in.web;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.validation.Errors;

import com.seheli.catalogue.domain.Fournisseur;

/**
 * Saisie du formulaire fournisseur. La remise est saisie en texte pour accepter la virgule
 * décimale ({@code 7,5}) comme le point ({@code 7.5}).
 */
public class FournisseurFormulaire {

    static final int NOM_LONGUEUR_MAX = 150;
    /** Pourcentage de 0 à 99,99 avec deux décimales au plus. */
    private static final Pattern REMISE = Pattern.compile("\\d{1,2}([.,]\\d{1,2})?");

    private String nom = "";
    private String remise = "";
    private boolean actif = true;

    static FournisseurFormulaire de(Fournisseur f) {
        FournisseurFormulaire formulaire = new FournisseurFormulaire();
        formulaire.nom = f.nom();
        formulaire.remise = f.remiseNegocieePct().stripTrailingZeros().toPlainString();
        formulaire.actif = f.estActif();
        return formulaire;
    }

    /** Vérifie la forme de la saisie ; les règles métier (bornes, unicité) sont contrôlées par le cas d'usage. */
    void valider(Errors erreurs) {
        String n = nom == null ? "" : nom.strip();
        if (n.isEmpty()) {
            erreurs.rejectValue("nom", "bo.formulaire.obligatoire");
        } else if (n.length() > NOM_LONGUEUR_MAX) {
            erreurs.rejectValue("nom", "bo.formulaire.trop.long", new Object[] {NOM_LONGUEUR_MAX}, null);
        }
        String r = remise == null ? "" : remise.strip();
        if (r.isEmpty()) {
            erreurs.rejectValue("remise", "bo.formulaire.obligatoire");
        } else if (!REMISE.matcher(r).matches()) {
            erreurs.rejectValue("remise", "bo.fournisseur.remise.invalide");
        }
    }

    /** Remise saisie, si elle est bien formée. */
    Optional<BigDecimal> remisePct() {
        String r = remise == null ? "" : remise.strip();
        if (!REMISE.matcher(r).matches()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(r.replace(',', '.')));
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getRemise() {
        return remise;
    }

    public void setRemise(String remise) {
        this.remise = remise;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
