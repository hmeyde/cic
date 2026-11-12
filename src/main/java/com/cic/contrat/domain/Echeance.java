package com.cic.contrat.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Mensualité d'un contrat : rang (à partir de 1), date de prélèvement (jour de paie du client),
 * montant fixé à la signature et statut. Le montant ne change jamais : aucune pénalité de retard.
 */
public final class Echeance {

    private final UUID id;
    private final int rang;
    private final LocalDate date;
    private final Montant montant;
    private final StatutEcheance statut;

    private Echeance(UUID id, int rang, LocalDate date, Montant montant, StatutEcheance statut) {
        this.id = Objects.requireNonNull(id, "id");
        this.date = Objects.requireNonNull(date, "date");
        this.montant = Objects.requireNonNull(montant, "montant");
        this.statut = Objects.requireNonNull(statut, "statut");
        if (rang <= 0) {
            throw new IllegalArgumentException("Rang d'échéance invalide : " + rang);
        }
        if (montant.estZero()) {
            throw new IllegalArgumentException("Une échéance a un montant strictement positif");
        }
        this.rang = rang;
    }

    /** Échéance A_VENIR tirée de l'échéancier fixé à la signature. */
    public static Echeance aVenir(EcheancePrevue prevue) {
        Objects.requireNonNull(prevue, "prevue");
        return new Echeance(UUID.randomUUID(), prevue.rang(), prevue.date(), prevue.montant(), StatutEcheance.A_VENIR);
    }

    public static Echeance reconstituer(UUID id, int rang, LocalDate date, Montant montant, StatutEcheance statut) {
        return new Echeance(id, rang, date, montant, statut);
    }

    public UUID id() {
        return id;
    }

    public int rang() {
        return rang;
    }

    public LocalDate date() {
        return date;
    }

    public Montant montant() {
        return montant;
    }

    public StatutEcheance statut() {
        return statut;
    }

    /** L'échéance est réglée. */
    public boolean estPayee() {
        return statut == StatutEcheance.PAYEE;
    }

    /**
     * Même échéance, réglée : A_VENIR ou EN_RETARD → PAYEE. Le montant ne change pas.
     *
     * @throws IllegalStateException l'échéance est déjà payée
     */
    public Echeance payee() {
        if (estPayee()) {
            throw new IllegalStateException("Échéance " + rang + " déjà payée");
        }
        return new Echeance(id, rang, date, montant, StatutEcheance.PAYEE);
    }

    /** L'échéance n'a pas été payée à sa date. */
    public boolean estEnRetard() {
        return statut == StatutEcheance.EN_RETARD;
    }

    /**
     * Même échéance, impayée à sa date : A_VENIR → EN_RETARD. Le montant ne change pas : aucune
     * pénalité de retard.
     *
     * @throws IllegalStateException l'échéance n'est pas A_VENIR
     */
    public Echeance enRetard() {
        if (statut != StatutEcheance.A_VENIR) {
            throw new IllegalStateException("Échéance " + rang + " " + statut + " : seule une échéance A_VENIR passe en retard");
        }
        return new Echeance(id, rang, date, montant, StatutEcheance.EN_RETARD);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Echeance e && id.equals(e.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Echeance[" + rang + ", " + date + ", " + montant + ", " + statut + "]";
    }
}
