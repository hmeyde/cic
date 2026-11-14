package com.seheli.identite.adapter.in.web;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.seheli.identite.domain.Document;
import com.seheli.identite.domain.TypeDocument;

/** Justificatif tel qu'affiché dans une liste : date à l'heure locale, taille en Ko. */
record LigneDocument(UUID id, TypeDocument type, String format, String recuLe, long tailleKo) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static LigneDocument depuis(Document d, Clock clock) {
        return new LigneDocument(d.id(), d.type(), d.format().name(),
                DATE.format(d.recuLe().atZone(clock.getZone())), Math.max(1, (d.tailleOctets() + 1023) / 1024));
    }
}
