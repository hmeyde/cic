package com.seheli.identite.domain;

/** Production des codes de connexion à usage unique : 6 chiffres (port de sortie). */
public interface GenerateurCode {

    int LONGUEUR = 6;

    /** Nouveau code de {@value #LONGUEUR} chiffres, zéros en tête compris. */
    String generer();
}
