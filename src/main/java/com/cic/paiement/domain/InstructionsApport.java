package com.cic.paiement.domain;

import java.util.Objects;

import com.cic.commun.domain.Montant;

/**
 * Ce que le client doit faire pour payer son apport : verser {@code montant} par Masrvi au code
 * marchand de CIC, en indiquant la référence de son dossier.
 */
public record InstructionsApport(Montant montant, String codeMarchand, ReferenceApport reference) {

    public InstructionsApport {
        Objects.requireNonNull(montant, "montant");
        Objects.requireNonNull(codeMarchand, "codeMarchand");
        Objects.requireNonNull(reference, "reference");
        if (codeMarchand.isBlank()) {
            throw new IllegalArgumentException("Code marchand Masrvi vide");
        }
        if (!montant.estSuperieurA(Montant.ZERO)) {
            throw new IllegalArgumentException("Apport nul ou négatif");
        }
    }
}
