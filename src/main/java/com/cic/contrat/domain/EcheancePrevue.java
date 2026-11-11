package com.cic.contrat.domain;

import java.time.LocalDate;
import java.util.Objects;

import com.cic.commun.domain.Montant;

/**
 * Mensualité prévue au contrat : rang (à partir de 1), date de prélèvement et montant.
 */
public record EcheancePrevue(int rang, LocalDate date, Montant montant) {

    public EcheancePrevue {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(montant, "montant");
        if (rang <= 0) {
            throw new IllegalArgumentException("Rang d'échéance invalide : " + rang);
        }
    }
}
