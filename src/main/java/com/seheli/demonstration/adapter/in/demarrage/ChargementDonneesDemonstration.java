package com.seheli.demonstration.adapter.in.demarrage;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.seheli.demonstration.application.DonneesDemonstrationService;

/**
 * Profil demo : charge les données de démonstration une fois l'application prête, c'est-à-dire après le
 * catalogue, le client et les comptes du back-office de démonstration (créés au démarrage).
 */
@Component
@Profile("demo")
class ChargementDonneesDemonstration {

    private final DonneesDemonstrationService donnees;

    ChargementDonneesDemonstration(DonneesDemonstrationService donnees) {
        this.donnees = donnees;
    }

    @EventListener(ApplicationReadyEvent.class)
    void charger() {
        donnees.chargerSiAbsentes();
    }
}
