package com.cic.catalogue.domain;

/**
 * Catégorie d'un produit. La marge CIC est fixée par catégorie ; tant qu'aucun paramètre
 * propre à la catégorie n'existe, c'est {@code marge.cic.defaut.pct} qui s'applique.
 */
public enum CategorieProduit {
    TELEPHONE,
    ELECTROMENAGER
}
