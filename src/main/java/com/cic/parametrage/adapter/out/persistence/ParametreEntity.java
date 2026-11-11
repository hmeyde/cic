package com.cic.parametrage.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parametre")
class ParametreEntity {

    @Id
    @Column(name = "cle", length = 100)
    private String cle;

    @Column(name = "valeur", nullable = false, length = 500)
    private String valeur;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "unite", length = 20)
    private String unite;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    protected ParametreEntity() {
    }

    String getCle() {
        return cle;
    }

    String getValeur() {
        return valeur;
    }

    String getType() {
        return type;
    }
}
