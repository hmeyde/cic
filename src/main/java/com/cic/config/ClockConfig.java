package com.cic.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Horloge unique de l'application, injectée partout où l'heure compte (fuseau de Nouakchott).
 *
 * <p>En profil demo, l'horloge vient du module demonstration : la même horloge système, qu'il fixe
 * dans le passé le temps de rejouer au démarrage l'historique des données de démonstration.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId FUSEAU = ZoneId.of("Africa/Nouakchott");

    @Bean
    @Profile("!demo")
    Clock clock() {
        return Clock.system(FUSEAU);
    }
}
