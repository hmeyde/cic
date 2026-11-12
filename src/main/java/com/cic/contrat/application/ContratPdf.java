package com.cic.contrat.application;

import java.util.Objects;

/** Contrat imprimé : nom du fichier proposé au téléchargement et contenu PDF. */
public record ContratPdf(String nomFichier, byte[] contenu) {

    public ContratPdf {
        Objects.requireNonNull(nomFichier, "nomFichier");
        Objects.requireNonNull(contenu, "contenu");
    }

    @Override
    public String toString() {
        return "ContratPdf[" + nomFichier + ", " + contenu.length + " octets]";
    }
}
