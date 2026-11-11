package com.cic.contrat.domain;

/**
 * Bornes du nombre de mensualités, lues dans la table {@code parametre}
 * ({@code duree.min.mois} et {@code duree.max.mois}).
 */
public record DureeAutorisee(int minMois, int maxMois) {

    public DureeAutorisee {
        if (minMois <= 0 || maxMois < minMois) {
            throw new IllegalArgumentException("Bornes de durée invalides : [" + minMois + " ; " + maxMois + "]");
        }
    }

    public boolean autorise(int nombreMois) {
        return nombreMois >= minMois && nombreMois <= maxMois;
    }
}
