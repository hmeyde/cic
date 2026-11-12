package com.cic.demonstration.domain;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Horloge de l'application en profil demo (port de sortie) : l'heure système, sauf pendant le chargement des
 * données de démonstration, où elle est fixée à l'instant de chaque étape rejouée.
 */
public interface HorlogeSimulee {

    /** Heure réelle, jamais simulée. */
    Instant maintenantReel();

    ZoneId zone();

    /** Toute l'application voit désormais cet instant. */
    void fixer(Instant instant);

    /** Retour à l'heure réelle. */
    void reprendre();
}
