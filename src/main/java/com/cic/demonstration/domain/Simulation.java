package com.cic.demonstration.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Simulation à événements discrets : des étapes datées, rejouées dans l'ordre chronologique, chacune avec
 * l'horloge de l'application fixée à son instant. Une étape peut en planifier d'autres, plus tard (par exemple
 * le paiement de chaque échéance, une fois le contrat établi).
 *
 * <p>Rien n'est rejoué dans le futur : une étape planifiée après l'heure réelle du début de la simulation est
 * ignorée. Les traitements planifiés de l'application prendront le relais à l'heure réelle.
 */
public final class Simulation {

    private final HorlogeSimulee horloge;
    private final Instant fin;
    private final ZoneId zone;
    private final PriorityQueue<Etape> etapes =
            new PriorityQueue<>(Comparator.comparing(Etape::le).thenComparingLong(Etape::ordre));
    private long ordre;
    private Instant courant = Instant.MIN;

    public Simulation(HorlogeSimulee horloge) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.fin = horloge.maintenantReel();
        this.zone = horloge.zone();
    }

    /** Date réelle du jour : les scénarios sont décrits par rapport à elle. */
    public LocalDate aujourdhui() {
        return LocalDate.ofInstant(fin, zone);
    }

    /**
     * Planifie une étape.
     *
     * @return {@code false} si l'instant est après l'heure réelle : l'étape est ignorée
     * @throws IllegalStateException si l'instant est avant l'étape en cours (scénario incohérent)
     */
    public boolean planifier(LocalDate jour, LocalTime heure, Runnable action) {
        Objects.requireNonNull(action, "action");
        Instant le = jour.atTime(heure).atZone(zone).toInstant();
        if (le.isAfter(fin)) {
            return false;
        }
        if (le.isBefore(courant)) {
            throw new IllegalStateException("Étape planifiée le " + le + ", avant l'étape en cours du " + courant);
        }
        etapes.add(new Etape(le, ordre++, action));
        return true;
    }

    /** Rejoue toutes les étapes, puis rend l'heure réelle à l'application, même en cas d'erreur. */
    public void derouler() {
        try {
            while (!etapes.isEmpty()) {
                Etape etape = etapes.poll();
                courant = etape.le();
                horloge.fixer(courant);
                etape.action().run();
            }
        } finally {
            horloge.reprendre();
        }
    }

    private record Etape(Instant le, long ordre, Runnable action) {
    }
}
