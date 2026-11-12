package com.cic.inscription.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import com.cic.inscription.domain.Inscription;
import com.cic.inscription.domain.TrancheSalaire;
import com.cic.inscription.domain.TypeContrat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inscription")
class InscriptionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nom", nullable = false, length = Inscription.NOM_LONGUEUR_MAX)
    private String nom;

    @Column(name = "telephone", nullable = false, length = 8)
    private String telephone;

    @Column(name = "employeur", nullable = false, length = Inscription.EMPLOYEUR_LONGUEUR_MAX)
    private String employeur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 20)
    private TypeContrat typeContrat;

    @Enumerated(EnumType.STRING)
    @Column(name = "tranche_salaire", nullable = false, length = 30)
    private TrancheSalaire trancheSalaire;

    @Column(name = "produit_souhaite", nullable = false, length = Inscription.PRODUIT_LONGUEUR_MAX)
    private String produitSouhaite;

    @Column(name = "consentement_donnees_le", nullable = false)
    private Instant consentementLe;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe;

    protected InscriptionEntity() {
    }

    InscriptionEntity(Inscription i) {
        this.id = i.id();
        this.nom = i.nom();
        this.telephone = i.telephone();
        this.employeur = i.employeur();
        this.typeContrat = i.typeContrat();
        this.trancheSalaire = i.trancheSalaire();
        this.produitSouhaite = i.produitSouhaite();
        this.consentementLe = i.consentementLe();
        this.creeLe = i.creeLe();
    }
}
