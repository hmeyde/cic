package com.cic.contrat.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Identité du client telle qu'elle figure sur le contrat. Données personnelles : jamais dans les
 * journaux ni dans les URL.
 */
public record Acheteur(String nom, String prenom, String nni, LocalDate dateNaissance, String telephone) {

    public Acheteur {
        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(prenom, "prenom");
        Objects.requireNonNull(nni, "nni");
        Objects.requireNonNull(dateNaissance, "dateNaissance");
        Objects.requireNonNull(telephone, "telephone");
    }

    @Override
    public String toString() {
        return "Acheteur[masqué]";
    }
}
