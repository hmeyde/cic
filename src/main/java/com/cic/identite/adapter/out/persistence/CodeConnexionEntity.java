package com.cic.identite.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "code_connexion")
class CodeConnexionEntity {

    @Id
    @Column(name = "telephone", length = 8)
    private String telephone;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "emis_le", nullable = false)
    private Instant emisLe;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "essais_max", nullable = false)
    private short essaisMax;

    @Column(name = "essais", nullable = false)
    private short essais;

    @Column(name = "utilise", nullable = false)
    private boolean utilise;

    protected CodeConnexionEntity() {
    }

    CodeConnexionEntity(String telephone, String codeHash, Instant emisLe, Instant expireLe, short essaisMax,
            short essais, boolean utilise) {
        this.telephone = telephone;
        this.codeHash = codeHash;
        this.emisLe = emisLe;
        this.expireLe = expireLe;
        this.essaisMax = essaisMax;
        this.essais = essais;
        this.utilise = utilise;
    }

    String getTelephone() {
        return telephone;
    }

    String getCodeHash() {
        return codeHash;
    }

    Instant getEmisLe() {
        return emisLe;
    }

    Instant getExpireLe() {
        return expireLe;
    }

    short getEssaisMax() {
        return essaisMax;
    }

    short getEssais() {
        return essais;
    }

    boolean isUtilise() {
        return utilise;
    }
}
