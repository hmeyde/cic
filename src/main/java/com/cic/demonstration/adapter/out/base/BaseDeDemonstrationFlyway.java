package com.cic.demonstration.adapter.out.base;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.cic.demonstration.domain.BaseDeDemonstration;

/**
 * Profil demo uniquement : remet la base à neuf avec Flyway ({@code clean} puis {@code migrate}), avec la
 * configuration de l'application. Le nettoyage reste interdit partout ailleurs ({@code cleanDisabled}).
 */
@Component
@Profile("demo")
class BaseDeDemonstrationFlyway implements BaseDeDemonstration {

    private final Flyway flyway;

    BaseDeDemonstrationFlyway(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void remettreANeuf() {
        Flyway nettoyable = Flyway.configure()
                .configuration(flyway.getConfiguration())
                .cleanDisabled(false)
                .load();
        nettoyable.clean();
        nettoyable.migrate();
    }
}
