package com.cic.recouvrement.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Relance envoyée au client pour une échéance. Une relance d'un type donné n'est envoyée qu'une
 * fois par échéance. Échéance et contrat référencés par identifiant (module contrat).
 */
public record Relance(UUID id, UUID echeanceId, UUID contratId, TypeRelance type, LocalDate dateEcheance,
        Instant envoyeeLe) {

    public Relance {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(echeanceId, "echeanceId");
        Objects.requireNonNull(contratId, "contratId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(dateEcheance, "dateEcheance");
        Objects.requireNonNull(envoyeeLe, "envoyeeLe");
    }

    public static Relance avantPrelevement(UUID echeanceId, UUID contratId, LocalDate dateEcheance, Instant le) {
        return new Relance(UUID.randomUUID(), echeanceId, contratId, TypeRelance.AVANT_PRELEVEMENT, dateEcheance, le);
    }
}
