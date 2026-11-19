package com.seheli.catalogue.adapter.in.web;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import com.seheli.catalogue.application.ProduitPropose;
import com.seheli.catalogue.domain.CategorieProduit;

/**
 * Visuel d'un produit dans le catalogue client : une illustration vectorielle (sous
 * {@code /img/produits/}) choisie d'après la désignation, sinon d'après la catégorie.
 *
 * <p>Le MVP ne stocke pas de photo par produit (pas de droits sur les visuels des marques) :
 * l'illustration donne un repère visuel sans dépendre d'un téléversement au back-office.
 */
final class IllustrationProduit {

    private static final String DOSSIER = "/img/produits/";

    /** Mot-clé (sans accent, en minuscules) recherché dans la désignation → fichier. */
    private static final List<String[]> PAR_MOT_CLE = List.of(
            new String[] {"refrigerateur", "refrigerateur.svg"},
            new String[] {"frigo", "refrigerateur.svg"},
            new String[] {"congelateur", "congelateur.svg"},
            new String[] {"climatiseur", "climatiseur.svg"},
            new String[] {"machine a laver", "machine-a-laver.svg"},
            new String[] {"lave-linge", "machine-a-laver.svg"},
            new String[] {"cuisiniere", "cuisiniere.svg"},
            new String[] {"fontaine", "fontaine.svg"});

    private IllustrationProduit() {
    }

    static String pour(ProduitPropose produit) {
        String designation = Normalizer.normalize(produit.designation(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        for (String[] regle : PAR_MOT_CLE) {
            if (designation.contains(regle[0])) {
                return DOSSIER + regle[1];
            }
        }
        return DOSSIER + (produit.categorie() == CategorieProduit.TELEPHONE ? "telephone.svg" : "electromenager.svg");
    }
}
