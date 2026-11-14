package com.seheli.demande.domain;

import java.util.Objects;

/**
 * Score complet d'une demande : conditions éliminatoires et note sur 100. Un dossier
 * inéligible est recommandé DEFAVORABLE quelle que soit sa note, qui reste visible.
 * Le score recommande ; l'analyste décide toujours.
 */
public record Score(Eligibilite eligibilite, NoteScore note) {

    public Score {
        Objects.requireNonNull(eligibilite, "eligibilite");
        Objects.requireNonNull(note, "note");
    }

    public Recommandation recommandation() {
        return eligibilite.estEligible() ? note.recommandation() : Recommandation.DEFAVORABLE;
    }

    public String versionGrille() {
        return note.versionGrille();
    }
}
