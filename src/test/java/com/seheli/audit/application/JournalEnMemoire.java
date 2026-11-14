package com.seheli.audit.application;

import java.util.ArrayList;
import java.util.List;

import com.seheli.audit.domain.EntreeJournal;
import com.seheli.audit.domain.JournalAuditRepository;
import com.seheli.audit.domain.ObjetAudite;

/** Journal d'audit en mémoire, dans l'ordre d'écriture. */
public class JournalEnMemoire implements JournalAuditRepository {

    private final List<EntreeJournal> entrees = new ArrayList<>();

    @Override
    public void ajouter(EntreeJournal entree) {
        entrees.add(entree);
    }

    @Override
    public List<EntreeJournal> parObjet(ObjetAudite objet) {
        return entrees.stream().filter(e -> e.objet().equals(objet)).toList();
    }

    public List<EntreeJournal> entrees() {
        return List.copyOf(entrees);
    }
}
