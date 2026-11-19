package com.seheli.contrat.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import com.seheli.commun.domain.Montant;

/**
 * Situation salariale du client utile à une simulation : salaire net (contrôle d'endettement),
 * jour de paie (dates des échéances) et, pour un CDD, date de fin du contrat de travail.
 */
public record SituationPaie(Montant salaireNet, int jourPaie, Optional<LocalDate> finContrat) {

    public SituationPaie {
        Objects.requireNonNull(salaireNet, "salaireNet");
        Objects.requireNonNull(finContrat, "finContrat");
        if (jourPaie < 1 || jourPaie > 31) {
            throw new IllegalArgumentException("Jour de paie invalide : " + jourPaie);
        }
    }
}
