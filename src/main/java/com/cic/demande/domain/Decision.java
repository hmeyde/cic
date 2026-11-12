package com.cic.demande.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Décision de l'analyste sur un dossier : qui l'a prise, quand, et le motif d'un refus. Conservée
 * pour la séparation des tâches : la personne qui décide d'un dossier ne valide pas son paiement
 * fournisseur.
 *
 * @param analyste identifiant de connexion du back-office
 * @param motif    motif du refus ; vide pour une acceptation
 */
public record Decision(String analyste, Instant le, Optional<String> motif) {

    public static final int LONGUEUR_ANALYSTE_MAX = 100;

    public Decision {
        Objects.requireNonNull(analyste, "analyste");
        Objects.requireNonNull(le, "le");
        Objects.requireNonNull(motif, "motif");
        if (analyste.isBlank() || analyste.length() > LONGUEUR_ANALYSTE_MAX) {
            throw new IllegalArgumentException("Identifiant d'analyste vide ou trop long");
        }
    }

    public boolean prisePar(String identifiant) {
        return analyste.equals(identifiant);
    }
}
