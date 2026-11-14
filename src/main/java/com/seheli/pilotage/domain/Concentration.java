package com.seheli.pilotage.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import com.seheli.commun.domain.Montant;

/**
 * Plus forte concentration de l'encours sur un même employeur ou fournisseur.
 *
 * @param libelle nom de l'employeur ou du fournisseur ; vide pour les clients dont l'employeur n'est
 *                pas renseigné
 * @param partPct part de l'encours total, en %, arrondie au dixième
 */
public record Concentration(Optional<String> libelle, Montant montant, BigDecimal partPct) {

    public Concentration {
        Objects.requireNonNull(libelle, "libelle");
        Objects.requireNonNull(montant, "montant");
        Objects.requireNonNull(partPct, "partPct");
    }
}
