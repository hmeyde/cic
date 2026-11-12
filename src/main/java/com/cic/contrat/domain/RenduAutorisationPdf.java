package com.cic.contrat.domain;

/** Mise en page de l'autorisation de prélèvement en PDF, en français (port de sortie). */
public interface RenduAutorisationPdf {

    /**
     * @return le document PDF, prêt à être téléchargé
     * @throws IllegalArgumentException type d'autorisation dont la mise en page n'existe pas encore
     */
    byte[] rendre(ContenuAutorisation contenu);
}
