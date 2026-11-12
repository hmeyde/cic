package com.cic.recouvrement.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.cic.recouvrement.domain.NoteRecouvrement;

/** Note du recouvrement, immuable ; dossier référencé par identifiant (module demande, sans relation JPA). */
@Entity
@Table(name = "note_recouvrement")
class NoteRecouvrementEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "demande_id", nullable = false, updatable = false)
    private UUID demandeId;

    @Column(name = "auteur", nullable = false, updatable = false, length = 100)
    private String auteur;

    @Column(name = "texte", nullable = false, updatable = false, length = NoteRecouvrement.LONGUEUR_MAX)
    private String texte;

    @Column(name = "ecrite_le", nullable = false, updatable = false)
    private Instant ecriteLe;

    protected NoteRecouvrementEntity() {
    }

    NoteRecouvrementEntity(NoteRecouvrement n) {
        this.id = n.id();
        this.demandeId = n.demandeId();
        this.auteur = n.auteur();
        this.texte = n.texte();
        this.ecriteLe = n.ecriteLe();
    }

    NoteRecouvrement versDomaine() {
        return new NoteRecouvrement(id, demandeId, auteur, texte, ecriteLe);
    }
}
