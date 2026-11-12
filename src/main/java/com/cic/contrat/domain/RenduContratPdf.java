package com.cic.contrat.domain;

/** Mise en page du contrat de vente à tempérament en PDF, en français (port de sortie). */
public interface RenduContratPdf {

    /** @return le document PDF, prêt à être téléchargé */
    byte[] rendre(ContenuContrat contenu);
}
