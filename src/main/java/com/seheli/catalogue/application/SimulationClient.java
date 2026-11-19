package com.seheli.catalogue.application;

import java.util.Objects;

import com.seheli.contrat.domain.SimulationDetaillee;

/**
 * Simulation affichée au client avec la position du curseur d'apport.
 *
 * @param detail       échéancier, décomposition du prix et alertes
 * @param apportPct    apport retenu en % du prix de vente
 * @param apportPctMin borne basse du curseur (apport minimum)
 * @param apportPctMax borne haute du curseur
 */
public record SimulationClient(SimulationDetaillee detail, int apportPct, int apportPctMin, int apportPctMax) {

    public SimulationClient {
        Objects.requireNonNull(detail, "detail");
    }
}
