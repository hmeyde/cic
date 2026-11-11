package com.cic.identite.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Compte d'un membre de l'équipe CIC dans le back-office.
 *
 * <p>Le domaine ne connaît que le hachage du mot de passe, jamais le mot de passe en clair.
 * L'identifiant est normalisé en minuscules : {@code Admin} et {@code admin} désignent le même compte.
 */
public final class UtilisateurBackOffice {

    private static final Pattern IDENTIFIANT = Pattern.compile("[a-z0-9._-]{3,50}");

    private final UUID id;
    private final String identifiant;
    private String motDePasseHash;
    private RoleBackOffice role;
    private boolean actif;

    private UtilisateurBackOffice(UUID id, String identifiant, String motDePasseHash, RoleBackOffice role,
            boolean actif) {
        this.id = Objects.requireNonNull(id, "id");
        this.identifiant = normaliser(identifiant);
        this.motDePasseHash = hashValide(motDePasseHash);
        this.role = Objects.requireNonNull(role, "role");
        this.actif = actif;
    }

    /** Nouveau compte, actif. */
    public static UtilisateurBackOffice nouveau(String identifiant, String motDePasseHash, RoleBackOffice role) {
        return new UtilisateurBackOffice(UUID.randomUUID(), identifiant, motDePasseHash, role, true);
    }

    /** Reconstitution depuis le stockage. */
    public static UtilisateurBackOffice reconstituer(UUID id, String identifiant, String motDePasseHash,
            RoleBackOffice role, boolean actif) {
        return new UtilisateurBackOffice(id, identifiant, motDePasseHash, role, actif);
    }

    /**
     * Forme normalisée d'un identifiant (minuscules, sans espaces autour).
     *
     * @throws IllegalArgumentException si l'identifiant ne contient pas 3 à 50 caractères
     *         parmi {@code a-z 0-9 . _ -}
     */
    public static String normaliser(String identifiant) {
        Objects.requireNonNull(identifiant, "identifiant");
        String i = identifiant.strip().toLowerCase(Locale.ROOT);
        if (!IDENTIFIANT.matcher(i).matches()) {
            throw new IllegalArgumentException("Identifiant invalide : 3 à 50 caractères parmi a-z 0-9 . _ -");
        }
        return i;
    }

    public void changerMotDePasse(String nouveauHash) {
        this.motDePasseHash = hashValide(nouveauHash);
    }

    public void changerRole(RoleBackOffice nouveauRole) {
        this.role = Objects.requireNonNull(nouveauRole, "role");
    }

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    public UUID id() {
        return id;
    }

    public String identifiant() {
        return identifiant;
    }

    public String motDePasseHash() {
        return motDePasseHash;
    }

    public RoleBackOffice role() {
        return role;
    }

    public boolean estActif() {
        return actif;
    }

    private static String hashValide(String hash) {
        Objects.requireNonNull(hash, "motDePasseHash");
        if (hash.isBlank()) {
            throw new IllegalArgumentException("Hachage du mot de passe vide");
        }
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UtilisateurBackOffice u && id.equals(u.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** N'expose jamais le hachage du mot de passe. */
    @Override
    public String toString() {
        return "UtilisateurBackOffice[" + id + ", " + role + (actif ? "" : ", inactif") + "]";
    }
}
