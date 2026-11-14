package com.seheli.identite.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeContrat;

/** Le client est référencé par identifiant (clé étrangère en base, sans relation JPA). */
@Entity
@Table(name = "emploi")
class EmploiEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_id", nullable = false, unique = true, updatable = false)
    private UUID clientId;

    @Column(name = "employeur", nullable = false, length = 150)
    private String employeur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 20)
    private TypeContrat typeContrat;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "salaire_net", nullable = false, precision = 12, scale = 0)
    private BigDecimal salaireNet;

    @Column(name = "jour_paie", nullable = false)
    private short jourPaie;

    @Column(name = "salaire_verse_sur_compte", nullable = false)
    private boolean salaireVerseSurCompte;

    @Column(name = "salaire_domicilie_bmci", nullable = false)
    private boolean salaireDomicilieBmci;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_remboursement", nullable = false, length = 20)
    private ModeRemboursement modeRemboursement;

    protected EmploiEntity() {
    }

    EmploiEntity(UUID id, UUID clientId, String employeur, TypeContrat typeContrat, LocalDate dateDebut,
            LocalDate dateFin, BigDecimal salaireNet, short jourPaie, boolean salaireVerseSurCompte,
            boolean salaireDomicilieBmci, ModeRemboursement modeRemboursement) {
        this.id = id;
        this.clientId = clientId;
        this.employeur = employeur;
        this.typeContrat = typeContrat;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.salaireNet = salaireNet;
        this.jourPaie = jourPaie;
        this.salaireVerseSurCompte = salaireVerseSurCompte;
        this.salaireDomicilieBmci = salaireDomicilieBmci;
        this.modeRemboursement = modeRemboursement;
    }

    UUID getId() {
        return id;
    }

    UUID getClientId() {
        return clientId;
    }

    String getEmployeur() {
        return employeur;
    }

    TypeContrat getTypeContrat() {
        return typeContrat;
    }

    LocalDate getDateDebut() {
        return dateDebut;
    }

    LocalDate getDateFin() {
        return dateFin;
    }

    BigDecimal getSalaireNet() {
        return salaireNet;
    }

    short getJourPaie() {
        return jourPaie;
    }

    boolean isSalaireVerseSurCompte() {
        return salaireVerseSurCompte;
    }

    ModeRemboursement getModeRemboursement() {
        return modeRemboursement;
    }

    boolean isSalaireDomicilieBmci() {
        return salaireDomicilieBmci;
    }
}
