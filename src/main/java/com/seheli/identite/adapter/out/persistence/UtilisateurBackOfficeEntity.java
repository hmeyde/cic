package com.seheli.identite.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.seheli.identite.domain.RoleBackOffice;

@Entity
@Table(name = "utilisateur_back_office")
class UtilisateurBackOfficeEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "identifiant", nullable = false, length = 50)
    private String identifiant;

    @Column(name = "mot_de_passe_hash", nullable = false, length = 100)
    private String motDePasseHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private RoleBackOffice role;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    protected UtilisateurBackOfficeEntity() {
    }

    UtilisateurBackOfficeEntity(UUID id, String identifiant, String motDePasseHash, RoleBackOffice role,
            boolean actif) {
        this.id = id;
        this.identifiant = identifiant;
        this.motDePasseHash = motDePasseHash;
        this.role = role;
        this.actif = actif;
    }

    UUID getId() {
        return id;
    }

    String getIdentifiant() {
        return identifiant;
    }

    String getMotDePasseHash() {
        return motDePasseHash;
    }

    RoleBackOffice getRole() {
        return role;
    }

    boolean isActif() {
        return actif;
    }
}
