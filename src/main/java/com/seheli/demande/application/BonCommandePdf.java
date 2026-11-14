package com.seheli.demande.application;

import java.util.Objects;

/** Bon de commande fournisseur en PDF, prêt à être téléchargé. */
public record BonCommandePdf(String nomFichier, byte[] contenu) {

    public BonCommandePdf {
        Objects.requireNonNull(nomFichier, "nomFichier");
        Objects.requireNonNull(contenu, "contenu");
    }
}
