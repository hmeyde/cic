package com.seheli.contrat.application;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.Prix;
import com.seheli.contrat.domain.Simulation;

/**
 * Simule un achat à crédit avec les paramètres en vigueur : prix calculé par
 * {@link CalculPrixService}, durée bornée par {@code duree.min.mois} – {@code duree.max.mois}.
 */
@Service
public class SimulationService {

    private final CalculPrixService calculPrix;
    private final EcheancierService echeancier;

    public SimulationService(CalculPrixService calculPrix, EcheancierService echeancier) {
        this.calculPrix = calculPrix;
        this.echeancier = echeancier;
    }

    /**
     * @throws IllegalArgumentException si la remise ou la durée sort des bornes en vigueur
     */
    public Simulation simuler(Montant prixPublic, BigDecimal remiseFournisseurPct, int nombreMois) {
        return simuler(calculPrix.calculer(prixPublic, remiseFournisseurPct), nombreMois);
    }

    /** @throws IllegalArgumentException si la durée sort des bornes en vigueur */
    public Simulation simuler(Prix prix, int nombreMois) {
        return Simulation.simuler(prix, nombreMois, echeancier.dureeAutorisee());
    }
}
