package com.cic.pilotage.application;

import java.util.Objects;

import com.cic.commun.domain.Montant;
import com.cic.pilotage.domain.IndicateursRisque;

/**
 * Tableau de bord des associés.
 *
 * @param capitalApporte  somme des apports en capital des associés
 * @param capitalEngage   argent avancé aux fournisseurs et pas encore revenu des clients
 * @param encours         reste dû par les clients sur les dossiers dont le fournisseur est payé
 * @param beneficeRealise marge déjà encaissée, au-delà du prix d'achat
 * @param risque          indicateurs de risque et alertes sur les seuils dépassés
 */
public record TableauDeBord(Montant capitalApporte, Montant capitalEngage, Montant encours,
        Montant beneficeRealise, IndicateursRisque risque) {

    public TableauDeBord {
        Objects.requireNonNull(capitalApporte, "capitalApporte");
        Objects.requireNonNull(capitalEngage, "capitalEngage");
        Objects.requireNonNull(encours, "encours");
        Objects.requireNonNull(beneficeRealise, "beneficeRealise");
        Objects.requireNonNull(risque, "risque");
    }
}
