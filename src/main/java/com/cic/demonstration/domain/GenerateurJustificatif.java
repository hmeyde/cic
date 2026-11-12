package com.cic.demonstration.domain;

/** Fabrique l'image d'un justificatif fictif, marqué comme tel (port de sortie). */
public interface GenerateurJustificatif {

    /**
     * @param titre  nature du document, par exemple « Pièce d'identité »
     * @param lignes contenu affiché sous le titre
     * @return une image PNG
     */
    byte[] png(String titre, String... lignes);
}
