package com.seheli;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Vérifie la structure modulaire : pas de cycle entre modules et aucun accès
 * à un type interne d'un autre module (seules les interfaces nommées sont exposées).
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(SeheliApplication.class);

    @Test
    void lesModulesRespectentLeursFrontieres() {
        modules.verify();
    }
}
