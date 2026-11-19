package com.seheli.pilotage.application;

import java.util.List;
import java.util.Objects;

import com.seheli.commun.domain.Montant;
import com.seheli.pilotage.domain.IndicateursRisque;
import com.seheli.pilotage.domain.PointEncours;
import com.seheli.pilotage.domain.PointRecouvrement;

/**
 * Tableau de bord des associés.
 *
 * @param capitalApporte     somme des apports en capital des associés
 * @param capitalEngage      argent avancé aux fournisseurs et pas encore revenu des clients
 * @param encours            reste dû par les clients sur les dossiers dont le fournisseur est payé
 * @param beneficeRealise    marge déjà encaissée, au-delà du prix d'achat
 * @param risque             indicateurs de risque et alertes sur les seuils dépassés
 * @param courbeEncours      encours en fin de mois, du plus ancien au mois en cours
 * @param courbeRecouvrement recouvrement des échéances échues de chaque mois, mêmes mois
 */
public record TableauDeBord(Montant capitalApporte, Montant capitalEngage, Montant encours,
        Montant beneficeRealise, IndicateursRisque risque, List<PointEncours> courbeEncours,
        List<PointRecouvrement> courbeRecouvrement) {

    public TableauDeBord {
        Objects.requireNonNull(capitalApporte, "capitalApporte");
        Objects.requireNonNull(capitalEngage, "capitalEngage");
        Objects.requireNonNull(encours, "encours");
        Objects.requireNonNull(beneficeRealise, "beneficeRealise");
        Objects.requireNonNull(risque, "risque");
        courbeEncours = List.copyOf(courbeEncours);
        courbeRecouvrement = List.copyOf(courbeRecouvrement);
    }
}
