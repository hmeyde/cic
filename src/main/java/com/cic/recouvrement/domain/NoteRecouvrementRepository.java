package com.cic.recouvrement.domain;

import java.util.List;
import java.util.UUID;

/** Stockage des notes du recouvrement (port de sortie) ; une note n'est jamais modifiée. */
public interface NoteRecouvrementRepository {

    void enregistrer(NoteRecouvrement note);

    /** Notes du dossier, de la plus récente à la plus ancienne. */
    List<NoteRecouvrement> parDemande(UUID demandeId);
}
