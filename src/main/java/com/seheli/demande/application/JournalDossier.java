package com.seheli.demande.application;

import java.time.Instant;
import java.util.UUID;

import com.seheli.audit.domain.Acteur;
import com.seheli.audit.domain.EntreeJournal;
import com.seheli.audit.domain.ObjetAudite;
import com.seheli.demande.domain.StatutDossier;

/** Entrées du journal d'audit pour un dossier : objet de type {@value #TYPE_OBJET}, identifié par la demande. */
final class JournalDossier {

    static final String TYPE_OBJET = "DEMANDE";

    private JournalDossier() {
    }

    static ObjetAudite objet(UUID demandeId) {
        return new ObjetAudite(TYPE_OBJET, demandeId);
    }

    /** @param depuis {@code null} à la création du dossier */
    static EntreeJournal transition(UUID demandeId, Instant le, Acteur acteur, ActionDossier action,
            StatutDossier depuis, StatutDossier vers, String detail) {
        return EntreeJournal.transition(le, acteur, action.name(), objet(demandeId),
                depuis == null ? null : depuis.name(), vers.name(), detail);
    }

    static EntreeJournal action(UUID demandeId, Instant le, Acteur acteur, ActionDossier action, String detail) {
        return EntreeJournal.action(le, acteur, action.name(), objet(demandeId), detail);
    }
}
