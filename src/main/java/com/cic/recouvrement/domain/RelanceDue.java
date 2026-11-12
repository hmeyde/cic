package com.cic.recouvrement.domain;

import java.util.Objects;

import com.cic.contrat.domain.EcheanceAPrelever;

/** Relance à envoyer aujourd'hui pour une échéance impayée. */
public record RelanceDue(EcheanceAPrelever echeance, TypeRelance type) {

    public RelanceDue {
        Objects.requireNonNull(echeance, "echeance");
        Objects.requireNonNull(type, "type");
    }
}
