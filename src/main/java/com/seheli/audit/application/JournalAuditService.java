package com.seheli.audit.application;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.audit.domain.EntreeJournal;
import com.seheli.audit.domain.JournalAuditRepository;
import com.seheli.audit.domain.ObjetAudite;

/**
 * Journal d'audit. {@link #journaliser} rejoint la transaction de l'appelant : si l'action métier
 * échoue, l'entrée disparaît avec elle ; si l'écriture au journal échoue, l'action est annulée.
 */
@Service
public class JournalAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(JournalAuditService.class);

    private final JournalAuditRepository journal;

    public JournalAuditService(JournalAuditRepository journal) {
        this.journal = journal;
    }

    @Transactional
    public void journaliser(EntreeJournal entree) {
        Objects.requireNonNull(entree, "entree");
        journal.ajouter(entree);
        LOG.debug("Journal d'audit : {}", entree);
    }

    /** Historique de l'objet, du plus ancien au plus récent. */
    @Transactional(readOnly = true)
    public List<EntreeJournal> historique(ObjetAudite objet) {
        Objects.requireNonNull(objet, "objet");
        return journal.parObjet(objet);
    }
}
