package com.seheli.identite.adapter.out.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client")
class ClientEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nni", nullable = false, length = 10, unique = true, updatable = false)
    private String nni;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "telephone", nullable = false, length = 8, unique = true)
    private String telephone;

    @Column(name = "consentement_donnees_le", nullable = false, updatable = false)
    private Instant consentementDonneesLe;

    @Column(name = "nni_verifie", nullable = false)
    private boolean nniVerifie;

    @Column(name = "piece_identite_verifiee", nullable = false)
    private boolean pieceIdentiteVerifiee;

    protected ClientEntity() {
    }

    ClientEntity(UUID id, String nni, String nom, String prenom, LocalDate dateNaissance, String telephone,
            Instant consentementDonneesLe, boolean nniVerifie, boolean pieceIdentiteVerifiee) {
        this.id = id;
        this.nni = nni;
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.telephone = telephone;
        this.consentementDonneesLe = consentementDonneesLe;
        this.nniVerifie = nniVerifie;
        this.pieceIdentiteVerifiee = pieceIdentiteVerifiee;
    }

    UUID getId() {
        return id;
    }

    String getNni() {
        return nni;
    }

    String getNom() {
        return nom;
    }

    String getPrenom() {
        return prenom;
    }

    LocalDate getDateNaissance() {
        return dateNaissance;
    }

    String getTelephone() {
        return telephone;
    }

    Instant getConsentementDonneesLe() {
        return consentementDonneesLe;
    }

    boolean isNniVerifie() {
        return nniVerifie;
    }

    boolean isPieceIdentiteVerifiee() {
        return pieceIdentiteVerifiee;
    }
}
