package com.seheli.demonstration.adapter.out.horloge;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.seheli.config.ClockConfig;
import com.seheli.demonstration.domain.HorlogeSimulee;

/**
 * Profil demo : la {@link Clock} injectée dans toute l'application (fuseau de Nouakchott). Heure système,
 * sauf pendant le chargement des données de démonstration, qui la fixe à l'instant de chaque étape rejouée.
 */
@Component("clock")
@Profile("demo")
class HorlogeDemonstration extends Clock implements HorlogeSimulee {

    private final ZoneId zone;
    private volatile Instant fixe;

    HorlogeDemonstration() {
        this(ClockConfig.FUSEAU);
    }

    private HorlogeDemonstration(ZoneId zone) {
        this.zone = zone;
    }

    @Override
    public Instant instant() {
        Instant f = fixe;
        return f != null ? f : Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId autreZone) {
        Objects.requireNonNull(autreZone, "autreZone");
        if (autreZone.equals(zone)) {
            return this;
        }
        HorlogeDemonstration source = this;
        return new Clock() {
            @Override
            public Instant instant() {
                return source.instant();
            }

            @Override
            public ZoneId getZone() {
                return autreZone;
            }

            @Override
            public Clock withZone(ZoneId z) {
                return source.withZone(z);
            }
        };
    }

    @Override
    public Instant maintenantReel() {
        return Instant.now();
    }

    @Override
    public ZoneId zone() {
        return zone;
    }

    @Override
    public void fixer(Instant instant) {
        fixe = Objects.requireNonNull(instant, "instant");
    }

    @Override
    public void reprendre() {
        fixe = null;
    }
}
