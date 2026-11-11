package com.cic.identite.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Emploi actuel d'un client : employeur, contrat, salaire et jour de paie.
 *
 * <p>Un client a un seul emploi enregistré (son emploi actuel), référencé par l'identifiant
 * du client. La date de fin est obligatoire pour un CDD et interdite sinon. Le jour de paie
 * (1 à 31) fixe la date des échéances ; un 31 tombe au dernier jour des mois plus courts.
 */
public final class Emploi {

    static final int EMPLOYEUR_LONGUEUR_MAX = 150;

    private final UUID id;
    private final UUID clientId;
    private String employeur;
    private TypeContrat typeContrat;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Montant salaireNet;
    private int jourPaie;
    private boolean salaireVerseSurCompte;
    private boolean salaireDomicilieBmci;

    private Emploi(UUID id, UUID clientId, String employeur, TypeContrat typeContrat, LocalDate dateDebut,
            LocalDate dateFin, Montant salaireNet, int jourPaie, boolean salaireVerseSurCompte,
            boolean salaireDomicilieBmci) {
        this.id = Objects.requireNonNull(id, "id");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        definir(employeur, typeContrat, dateDebut, dateFin, salaireNet, jourPaie, salaireVerseSurCompte,
                salaireDomicilieBmci);
    }

    public static Emploi nouveau(UUID clientId, String employeur, TypeContrat typeContrat, LocalDate dateDebut,
            LocalDate dateFin, Montant salaireNet, int jourPaie, boolean salaireVerseSurCompte,
            boolean salaireDomicilieBmci) {
        return new Emploi(UUID.randomUUID(), clientId, employeur, typeContrat, dateDebut, dateFin, salaireNet,
                jourPaie, salaireVerseSurCompte, salaireDomicilieBmci);
    }

    /** Reconstitution depuis le stockage. */
    public static Emploi reconstituer(UUID id, UUID clientId, String employeur, TypeContrat typeContrat,
            LocalDate dateDebut, LocalDate dateFin, Montant salaireNet, int jourPaie,
            boolean salaireVerseSurCompte, boolean salaireDomicilieBmci) {
        return new Emploi(id, clientId, employeur, typeContrat, dateDebut, dateFin, salaireNet, jourPaie,
                salaireVerseSurCompte, salaireDomicilieBmci);
    }

    /** Remplace les informations d'emploi (changement d'employeur, de contrat ou de salaire). */
    public void modifier(String employeur, TypeContrat typeContrat, LocalDate dateDebut, LocalDate dateFin,
            Montant salaireNet, int jourPaie, boolean salaireVerseSurCompte, boolean salaireDomicilieBmci) {
        definir(employeur, typeContrat, dateDebut, dateFin, salaireNet, jourPaie, salaireVerseSurCompte,
                salaireDomicilieBmci);
    }

    private void definir(String employeur, TypeContrat typeContrat, LocalDate dateDebut, LocalDate dateFin,
            Montant salaireNet, int jourPaie, boolean salaireVerseSurCompte, boolean salaireDomicilieBmci) {
        Objects.requireNonNull(typeContrat, "typeContrat");
        Objects.requireNonNull(dateDebut, "dateDebut");
        Objects.requireNonNull(salaireNet, "salaireNet");
        if (typeContrat == TypeContrat.CDD) {
            if (dateFin == null) {
                throw new IllegalArgumentException("Un CDD doit avoir une date de fin");
            }
            if (!dateFin.isAfter(dateDebut)) {
                throw new IllegalArgumentException("La fin du contrat doit suivre son début");
            }
        } else if (dateFin != null) {
            throw new IllegalArgumentException("Seul un CDD a une date de fin");
        }
        if (salaireNet.estZero()) {
            throw new IllegalArgumentException("Le salaire net doit être positif");
        }
        if (jourPaie < 1 || jourPaie > 31) {
            throw new IllegalArgumentException("Jour de paie hors bornes (1 à 31) : " + jourPaie);
        }
        if (salaireDomicilieBmci && !salaireVerseSurCompte) {
            throw new IllegalArgumentException("Un salaire domicilié à la BMCI est versé sur un compte");
        }
        this.employeur = employeurValide(employeur);
        this.typeContrat = typeContrat;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.salaireNet = salaireNet;
        this.jourPaie = jourPaie;
        this.salaireVerseSurCompte = salaireVerseSurCompte;
        this.salaireDomicilieBmci = salaireDomicilieBmci;
    }

    /** Le contrat est en cours à cette date (commencé et, pour un CDD, pas encore terminé). */
    public boolean estEnCoursAu(LocalDate date) {
        return !dateDebut.isAfter(date) && (dateFin == null || !dateFin.isBefore(date));
    }

    public UUID id() {
        return id;
    }

    public UUID clientId() {
        return clientId;
    }

    public String employeur() {
        return employeur;
    }

    public TypeContrat typeContrat() {
        return typeContrat;
    }

    public LocalDate dateDebut() {
        return dateDebut;
    }

    /** Fin du contrat ; vide pour un CDI ou un fonctionnaire. */
    public Optional<LocalDate> dateFin() {
        return Optional.ofNullable(dateFin);
    }

    public Montant salaireNet() {
        return salaireNet;
    }

    public int jourPaie() {
        return jourPaie;
    }

    public boolean estSalaireVerseSurCompte() {
        return salaireVerseSurCompte;
    }

    public boolean estSalaireDomicilieBmci() {
        return salaireDomicilieBmci;
    }

    private static String employeurValide(String employeur) {
        Objects.requireNonNull(employeur, "employeur");
        String e = employeur.strip();
        if (e.isEmpty() || e.length() > EMPLOYEUR_LONGUEUR_MAX) {
            throw new IllegalArgumentException("Employeur vide ou trop long (max " + EMPLOYEUR_LONGUEUR_MAX + ")");
        }
        return e;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Emploi e && id.equals(e.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Pas de donnée personnelle (employeur, salaire) : ce texte peut finir dans les journaux. */
    @Override
    public String toString() {
        return "Emploi[" + id + ", client " + clientId + ", " + typeContrat + "]";
    }
}
