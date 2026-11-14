package com.seheli.recouvrement.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.seheli.recouvrement.domain.NoteRecouvrement;
import com.seheli.recouvrement.domain.NoteRecouvrementRepository;

public class NotesEnMemoire implements NoteRecouvrementRepository {

    private final List<NoteRecouvrement> notes = new ArrayList<>();

    @Override
    public void enregistrer(NoteRecouvrement note) {
        notes.add(note);
    }

    @Override
    public List<NoteRecouvrement> parDemande(UUID demandeId) {
        return notes.stream().filter(n -> n.demandeId().equals(demandeId))
                .sorted(Comparator.comparing(NoteRecouvrement::ecriteLe).reversed()).toList();
    }
}
