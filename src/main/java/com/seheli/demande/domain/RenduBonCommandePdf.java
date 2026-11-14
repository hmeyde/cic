package com.seheli.demande.domain;

/** Mise en page du bon de commande fournisseur en PDF, en français (port de sortie). */
public interface RenduBonCommandePdf {

    /** @return le document PDF, prêt à être téléchargé */
    byte[] rendre(ContenuBonCommande contenu);
}
