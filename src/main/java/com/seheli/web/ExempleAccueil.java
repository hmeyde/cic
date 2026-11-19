package com.seheli.web;

import java.math.BigDecimal;
import java.util.Objects;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.Simulation;

/**
 * Exemple chiffré et points clés de la page d'accueil, tirés du calcul du domaine
 * ({@link Simulation}) et des paramètres en vigueur : rien n'est écrit en dur dans le gabarit.
 *
 * @param apportPct taux d'apport minimum en vigueur, sans zéros inutiles (ex. « 40 »)
 * @param dureeMinMois nombre minimum de mensualités
 * @param dureeMaxMois nombre maximum de mensualités (durée de l'exemple)
 * @param simulation simulation de l'exemple
 */
public record ExempleAccueil(BigDecimal apportPct, int dureeMinMois, int dureeMaxMois, Simulation simulation) {

    public ExempleAccueil {
        Objects.requireNonNull(apportPct, "apportPct");
        Objects.requireNonNull(simulation, "simulation");
        apportPct = apportPct.stripTrailingZeros();
    }

    public Montant prixTotal() {
        return simulation.prixTotal();
    }

    public Montant apport() {
        return simulation.apport();
    }

    public Montant mensualite() {
        return simulation.mensualite();
    }

    public Montant derniereMensualite() {
        return simulation.derniereMensualite();
    }

    public int nombreMois() {
        return simulation.nombreMois();
    }

    /** Vrai si la dernière mensualité absorbe un écart d'arrondi. */
    public boolean derniereDiffere() {
        return simulation.derniereDiffere();
    }

    /** Taux d'apport prêt à afficher (« 40 », « 42.5 »). */
    public String apportPctTexte() {
        return apportPct.toPlainString();
    }
}
