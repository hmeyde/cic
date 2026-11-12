package com.cic.paiement.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Paiement reçu par CIC (Masrvi), saisi au back-office. Sa référence est unique. À la saisie, il est
 * rapproché par sa référence : s'il s'agit de la référence d'apport d'un dossier, il est rattaché à
 * ce dossier (contrat, dossier et client par identifiant) et garde l'apport attendu pour mettre en
 * évidence un écart de montant. Sinon il est enregistré quand même, NON_RAPPROCHE : l'argent est
 * reçu, il doit apparaître.
 *
 * <p>Un paiement ne se modifie pas : une erreur se corrige par une opération inverse, comme une
 * écriture comptable.
 */
public final class Paiement {

    public static final int AUTEUR_LONGUEUR_MAX = 100;

    private final UUID id;
    private final ReferencePaiement reference;
    private final Montant montant;
    private final LocalDate recuLe;
    private final ApportAttendu apportAttendu;
    private final String saisiPar;
    private final Instant saisiLe;

    private Paiement(UUID id, ReferencePaiement reference, Montant montant, LocalDate recuLe,
            ApportAttendu apportAttendu, String saisiPar, Instant saisiLe) {
        this.id = Objects.requireNonNull(id, "id");
        this.reference = Objects.requireNonNull(reference, "reference");
        this.montant = Objects.requireNonNull(montant, "montant");
        this.recuLe = Objects.requireNonNull(recuLe, "recuLe");
        this.apportAttendu = apportAttendu;
        this.saisiPar = Objects.requireNonNull(saisiPar, "saisiPar");
        this.saisiLe = Objects.requireNonNull(saisiLe, "saisiLe");
        if (!montant.estSuperieurA(Montant.ZERO)) {
            throw new IllegalArgumentException("Montant d'un paiement nul");
        }
        if (saisiPar.isBlank() || saisiPar.length() > AUTEUR_LONGUEUR_MAX) {
            throw new IllegalArgumentException("Auteur de la saisie vide ou trop long");
        }
    }

    /**
     * Paiement saisi au back-office, rapproché par sa référence.
     *
     * @param apportAttendu dossier désigné par la référence ; vide si elle n'en désigne aucun
     */
    public static Paiement saisir(ReferencePaiement reference, Montant montant, LocalDate recuLe,
            Optional<ApportAttendu> apportAttendu, String saisiPar, Instant saisiLe) {
        Objects.requireNonNull(apportAttendu, "apportAttendu");
        return new Paiement(UUID.randomUUID(), reference, montant, recuLe, apportAttendu.orElse(null), saisiPar,
                saisiLe);
    }

    public static Paiement reconstituer(UUID id, ReferencePaiement reference, Montant montant, LocalDate recuLe,
            Optional<ApportAttendu> apportAttendu, String saisiPar, Instant saisiLe) {
        return new Paiement(id, reference, montant, recuLe, apportAttendu.orElse(null), saisiPar, saisiLe);
    }

    public StatutRapprochement statut() {
        return apportAttendu == null ? StatutRapprochement.NON_RAPPROCHE : StatutRapprochement.RAPPROCHE;
    }

    public boolean estRapproche() {
        return apportAttendu != null;
    }

    /** Rapproché mais d'un montant différent de l'apport fixé au contrat. */
    public boolean ecartDeMontant() {
        return apportAttendu != null && montant.compareTo(apportAttendu.montant()) != 0;
    }

    public UUID id() {
        return id;
    }

    public ReferencePaiement reference() {
        return reference;
    }

    public Montant montant() {
        return montant;
    }

    /** Date du paiement chez Masrvi. */
    public LocalDate recuLe() {
        return recuLe;
    }

    /** Dossier auquel le paiement est rattaché ; vide s'il n'est pas rapproché. */
    public Optional<ApportAttendu> apportAttendu() {
        return Optional.ofNullable(apportAttendu);
    }

    /** Identifiant de connexion du membre du back-office qui a saisi le paiement. */
    public String saisiPar() {
        return saisiPar;
    }

    public Instant saisiLe() {
        return saisiLe;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Paiement p && id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Pas de donnée personnelle : ce texte peut finir dans les journaux. */
    @Override
    public String toString() {
        return "Paiement[" + id + ", " + statut()
                + (apportAttendu == null ? "" : ", contrat " + apportAttendu.numeroContrat()) + "]";
    }
}
