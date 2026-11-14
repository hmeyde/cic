package com.seheli.contrat.application;

import java.util.Objects;

import com.seheli.contrat.domain.DocumentSigne;

/** Document signé d'une autorisation et son contenu, pour l'afficher au back-office. */
public record DocumentSigneContenu(DocumentSigne document, byte[] contenu) {

    public DocumentSigneContenu {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(contenu, "contenu");
    }
}
