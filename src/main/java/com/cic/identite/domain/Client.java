package com.cic.identite.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Client de CIC : salarié qui achète à crédit.
 *
 * <p>Le NNI (numéro national d'identification, 10 chiffres) est unique : un NNI = un client.
 * Le téléphone (8 chiffres, numéro mauritanien sans indicatif) est aussi unique, car il sert
 * à recevoir le code de connexion à usage unique.
 *
 * <p>Le consentement explicite à l'usage des données est obligatoire à la création et horodaté.
 * Le NNI et la pièce d'identité sont vérifiés plus tard par le back-office.
 */
public final class Client {

    private static final Pattern NNI = Pattern.compile("\\d{10}");
    private static final Pattern TELEPHONE = Pattern.compile("\\d{8}");
    static final int NOM_LONGUEUR_MAX = 100;

    private final UUID id;
    private final String nni;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String telephone;
    private final Instant consentementDonneesLe;
    private boolean nniVerifie;
    private boolean pieceIdentiteVerifiee;

    private Client(UUID id, String nni, String nom, String prenom, LocalDate dateNaissance, String telephone,
            Instant consentementDonneesLe, boolean nniVerifie, boolean pieceIdentiteVerifiee) {
        this.id = Objects.requireNonNull(id, "id");
        this.nni = normaliserNni(nni);
        this.nom = texteValide(nom, "nom");
        this.prenom = texteValide(prenom, "prenom");
        this.dateNaissance = Objects.requireNonNull(dateNaissance, "dateNaissance");
        this.telephone = normaliserTelephone(telephone);
        this.consentementDonneesLe = Objects.requireNonNull(consentementDonneesLe, "consentementDonneesLe");
        this.nniVerifie = nniVerifie;
        this.pieceIdentiteVerifiee = pieceIdentiteVerifiee;
    }

    /**
     * Nouveau client, identité non encore vérifiée.
     *
     * @param consentementDonneesLe instant où le client a donné son consentement (horloge injectée)
     */
    public static Client nouveau(String nni, String nom, String prenom, LocalDate dateNaissance, String telephone,
            Instant consentementDonneesLe) {
        return new Client(UUID.randomUUID(), nni, nom, prenom, dateNaissance, telephone, consentementDonneesLe,
                false, false);
    }

    /** Reconstitution depuis le stockage. */
    public static Client reconstituer(UUID id, String nni, String nom, String prenom, LocalDate dateNaissance,
            String telephone, Instant consentementDonneesLe, boolean nniVerifie, boolean pieceIdentiteVerifiee) {
        return new Client(id, nni, nom, prenom, dateNaissance, telephone, consentementDonneesLe, nniVerifie,
                pieceIdentiteVerifiee);
    }

    /** Forme normalisée d'un NNI (sans espaces). */
    public static String normaliserNni(String nni) {
        Objects.requireNonNull(nni, "nni");
        String n = nni.replace(" ", "");
        if (!NNI.matcher(n).matches()) {
            throw new IllegalArgumentException("NNI invalide : 10 chiffres attendus");
        }
        return n;
    }

    /** Forme normalisée d'un téléphone (sans espaces ni indicatif +222). */
    public static String normaliserTelephone(String telephone) {
        Objects.requireNonNull(telephone, "telephone");
        String t = telephone.replace(" ", "");
        if (t.startsWith("+222")) {
            t = t.substring(4);
        } else if (t.startsWith("00222")) {
            t = t.substring(5);
        }
        if (!TELEPHONE.matcher(t).matches()) {
            throw new IllegalArgumentException("Téléphone invalide : 8 chiffres attendus");
        }
        return t;
    }

    /** Âge révolu à la date donnée. */
    public int ageAu(LocalDate date) {
        return Period.between(dateNaissance, date).getYears();
    }

    public void corrigerIdentite(String nouveauNom, String nouveauPrenom, LocalDate nouvelleDateNaissance) {
        this.nom = texteValide(nouveauNom, "nom");
        this.prenom = texteValide(nouveauPrenom, "prenom");
        this.dateNaissance = Objects.requireNonNull(nouvelleDateNaissance, "dateNaissance");
        // L'identité corrigée doit être vérifiée de nouveau.
        this.nniVerifie = false;
        this.pieceIdentiteVerifiee = false;
    }

    public void changerTelephone(String nouveauTelephone) {
        this.telephone = normaliserTelephone(nouveauTelephone);
    }

    public void verifierNni() {
        this.nniVerifie = true;
    }

    public void verifierPieceIdentite() {
        this.pieceIdentiteVerifiee = true;
    }

    public UUID id() {
        return id;
    }

    public String nni() {
        return nni;
    }

    public String nom() {
        return nom;
    }

    public String prenom() {
        return prenom;
    }

    public LocalDate dateNaissance() {
        return dateNaissance;
    }

    public String telephone() {
        return telephone;
    }

    public Instant consentementDonneesLe() {
        return consentementDonneesLe;
    }

    public boolean estNniVerifie() {
        return nniVerifie;
    }

    public boolean estPieceIdentiteVerifiee() {
        return pieceIdentiteVerifiee;
    }

    private static String texteValide(String valeur, String champ) {
        Objects.requireNonNull(valeur, champ);
        String v = valeur.strip();
        if (v.isEmpty() || v.length() > NOM_LONGUEUR_MAX) {
            throw new IllegalArgumentException(champ + " vide ou trop long (max " + NOM_LONGUEUR_MAX + ")");
        }
        return v;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Client c && id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Pas de donnée personnelle (NNI, nom, téléphone) : ce texte peut finir dans les journaux. */
    @Override
    public String toString() {
        return "Client[" + id + "]";
    }
}
