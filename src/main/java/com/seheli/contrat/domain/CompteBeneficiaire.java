package com.seheli.contrat.domain;

import java.util.Objects;

/** Compte bancaire de Seheli qui reçoit les mensualités : titulaire, banque et numéro (RIB). */
public record CompteBeneficiaire(String titulaire, String banque, String numero) {

    public CompteBeneficiaire {
        Objects.requireNonNull(titulaire, "titulaire");
        Objects.requireNonNull(banque, "banque");
        Objects.requireNonNull(numero, "numero");
    }
}
