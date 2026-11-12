package com.cic.audit.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import com.cic.audit.domain.TypeActeur;

/**
 * Entrée du journal d'audit (table {@code journal_audit}). Immuable : Hibernate n'émet jamais
 * d'UPDATE et la base refuse toute modification ou suppression. L'objet audité est référencé par
 * type et identifiant, sans relation JPA vers son module.
 */
@Entity
@Immutable
@Table(name = "journal_audit")
class JournalAuditEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    /** Ordre d'écriture, attribué par la base : départage deux entrées au même instant. */
    @Column(name = "numero", insertable = false, updatable = false)
    private Long numero;

    @Column(name = "le", nullable = false, updatable = false)
    private Instant le;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_acteur", nullable = false, updatable = false, length = 20)
    private TypeActeur typeActeur;

    @Column(name = "acteur", nullable = false, updatable = false, length = 100)
    private String acteur;

    @Column(name = "action", nullable = false, updatable = false, length = 50)
    private String action;

    @Column(name = "type_objet", nullable = false, updatable = false, length = 50)
    private String typeObjet;

    @Column(name = "objet_id", nullable = false, updatable = false)
    private UUID objetId;

    @Column(name = "avant", updatable = false, length = 50)
    private String avant;

    @Column(name = "apres", updatable = false, length = 50)
    private String apres;

    @Column(name = "detail", updatable = false, length = 1000)
    private String detail;

    protected JournalAuditEntity() {
    }

    JournalAuditEntity(UUID id, Instant le, TypeActeur typeActeur, String acteur, String action, String typeObjet,
            UUID objetId, String avant, String apres, String detail) {
        this.id = id;
        this.le = le;
        this.typeActeur = typeActeur;
        this.acteur = acteur;
        this.action = action;
        this.typeObjet = typeObjet;
        this.objetId = objetId;
        this.avant = avant;
        this.apres = apres;
        this.detail = detail;
    }

    UUID getId() {
        return id;
    }

    Instant getLe() {
        return le;
    }

    TypeActeur getTypeActeur() {
        return typeActeur;
    }

    String getActeur() {
        return acteur;
    }

    String getAction() {
        return action;
    }

    String getTypeObjet() {
        return typeObjet;
    }

    UUID getObjetId() {
        return objetId;
    }

    String getAvant() {
        return avant;
    }

    String getApres() {
        return apres;
    }

    String getDetail() {
        return detail;
    }
}
