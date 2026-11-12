package com.cic.recouvrement.adapter.out.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.cic.recouvrement.domain.Relance;
import com.cic.recouvrement.domain.TypeRelance;

/** Relance envoyée ; échéance et contrat référencés par identifiant (module contrat, sans relation JPA). */
@Entity
@Table(name = "relance")
class RelanceEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "echeance_id", nullable = false, updatable = false)
    private UUID echeanceId;

    @Column(name = "contrat_id", nullable = false, updatable = false)
    private UUID contratId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 30)
    private TypeRelance type;

    @Column(name = "date_echeance", nullable = false, updatable = false)
    private LocalDate dateEcheance;

    @Column(name = "envoyee_le", nullable = false, updatable = false)
    private Instant envoyeeLe;

    protected RelanceEntity() {
    }

    RelanceEntity(UUID id, UUID echeanceId, UUID contratId, TypeRelance type, LocalDate dateEcheance,
            Instant envoyeeLe) {
        this.id = id;
        this.echeanceId = echeanceId;
        this.contratId = contratId;
        this.type = type;
        this.dateEcheance = dateEcheance;
        this.envoyeeLe = envoyeeLe;
    }

    Relance versDomaine() {
        return new Relance(id, echeanceId, contratId, type, dateEcheance, envoyeeLe);
    }
}
