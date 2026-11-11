package com.cic.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Horloge unique de l'application, injectée partout où l'heure compte (fuseau de Nouakchott). */
@Configuration
public class ClockConfig {

    public static final ZoneId FUSEAU = ZoneId.of("Africa/Nouakchott");

    @Bean
    Clock clock() {
        return Clock.system(FUSEAU);
    }
}
