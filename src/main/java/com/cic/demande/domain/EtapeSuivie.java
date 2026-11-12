package com.cic.demande.domain;

import java.util.Objects;

/** Une étape du parcours et son avancement. */
public record EtapeSuivie(EtapeDossier etape, EtatEtape etat) {

    public EtapeSuivie {
        Objects.requireNonNull(etape, "etape");
        Objects.requireNonNull(etat, "etat");
    }
}
