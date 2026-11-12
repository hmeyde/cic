package com.cic.comptabilite.adapter.in.web;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.cic.commun.domain.Montant;
import com.cic.comptabilite.domain.ApportCapital;

/** Apport en capital tel qu'affiché au back-office, dates déjà formatées. */
record VueApportCapital(UUID id, String associe, Montant montant, String verseLe, String saisiLe, String saisiPar) {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static VueApportCapital depuis(ApportCapital a, Clock clock) {
        return new VueApportCapital(a.id(), a.associe(), a.montant(), DATE.format(a.verseLe()),
                DATE_HEURE.format(a.saisiLe().atZone(clock.getZone())), a.saisiPar());
    }
}
