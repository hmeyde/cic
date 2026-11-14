package com.seheli.comptabilite.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import com.seheli.commun.domain.Montant;
import com.seheli.comptabilite.domain.ApportCapital;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Apport en capital d'un associé, immuable. */
@Entity
@Immutable
@Table(name = "apport_capital")
class ApportCapitalEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "associe", nullable = false, updatable = false, length = ApportCapital.ASSOCIE_LONGUEUR_MAX)
    private String associe;

    @Column(name = "montant", nullable = false, updatable = false, precision = 14, scale = 0)
    private BigDecimal montant;

    @Column(name = "verse_le", nullable = false, updatable = false)
    private LocalDate verseLe;

    @Column(name = "saisi_par", nullable = false, updatable = false, length = 100)
    private String saisiPar;

    @Column(name = "saisi_le", nullable = false, updatable = false)
    private Instant saisiLe;

    protected ApportCapitalEntity() {
    }

    ApportCapitalEntity(ApportCapital a) {
        this.id = a.id();
        this.associe = a.associe();
        this.montant = a.montant().valeur();
        this.verseLe = a.verseLe();
        this.saisiPar = a.saisiPar();
        this.saisiLe = a.saisiLe();
    }

    ApportCapital versDomaine() {
        return new ApportCapital(id, associe, Montant.de(montant), verseLe, saisiPar, saisiLe);
    }
}
