package com.seheli.inscription.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Pré-inscription d'un prospect depuis la page publique (preuve de concept) : elle mesure l'intérêt
 * des salariés et permet à Seheli de les rappeler. Ce n'est pas un compte client : aucune pièce, aucun
 * NNI. Le téléphone, normalisé sur 8 chiffres sans indicatif, identifie le prospect (anti-doublon).
 * Le consentement à l'usage des données est obligatoire et horodaté.
 */
public record Inscription(UUID id, String nom, String telephone, String employeur, TypeContrat typeContrat,
        TrancheSalaire trancheSalaire, String produitSouhaite, Instant consentementLe, Instant creeLe) {

    public static final int NOM_LONGUEUR_MAX = 100;
    public static final int EMPLOYEUR_LONGUEUR_MAX = 150;
    public static final int PRODUIT_LONGUEUR_MAX = 150;

    private static final Pattern TELEPHONE = Pattern.compile("\\d{8}");

    public Inscription {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(telephone, "telephone");
        Objects.requireNonNull(employeur, "employeur");
        Objects.requireNonNull(typeContrat, "typeContrat");
        Objects.requireNonNull(trancheSalaire, "trancheSalaire");
        Objects.requireNonNull(produitSouhaite, "produitSouhaite");
        Objects.requireNonNull(consentementLe, "consentementLe");
        Objects.requireNonNull(creeLe, "creeLe");
    }

    /**
     * Nouvelle pré-inscription ; les textes sont débarrassés de leurs espaces superflus.
     *
     * @param consentement le prospect a coché la case de consentement
     * @param le           instant de l'envoi du formulaire, qui horodate aussi le consentement
     * @throws InscriptionInvalideException un champ manque ou est mal formé
     */
    public static Inscription nouvelle(String nom, String telephone, String employeur, TypeContrat typeContrat,
            TrancheSalaire trancheSalaire, String produitSouhaite, boolean consentement, Instant le) {
        Objects.requireNonNull(le, "le");
        String n = texte(nom, NOM_LONGUEUR_MAX, InscriptionInvalideException.NOM_INVALIDE);
        String t = normaliserTelephone(telephone);
        String e = texte(employeur, EMPLOYEUR_LONGUEUR_MAX, InscriptionInvalideException.EMPLOYEUR_INVALIDE);
        if (typeContrat == null) {
            throw new InscriptionInvalideException(InscriptionInvalideException.TYPE_CONTRAT_INVALIDE);
        }
        if (trancheSalaire == null) {
            throw new InscriptionInvalideException(InscriptionInvalideException.TRANCHE_SALAIRE_INVALIDE);
        }
        String p = texte(produitSouhaite, PRODUIT_LONGUEUR_MAX, InscriptionInvalideException.PRODUIT_INVALIDE);
        if (!consentement) {
            throw new InscriptionInvalideException(InscriptionInvalideException.CONSENTEMENT_MANQUANT);
        }
        return new Inscription(UUID.randomUUID(), n, t, e, typeContrat, trancheSalaire, p, le, le);
    }

    /**
     * Forme normalisée d'un téléphone mauritanien : 8 chiffres, sans espaces, tirets ni indicatif +222.
     *
     * @throws InscriptionInvalideException numéro mal formé
     */
    public static String normaliserTelephone(String telephone) {
        String t = telephone == null ? "" : telephone.replaceAll("[\\s\\u00A0\\u202F.-]", "");
        if (t.startsWith("+222")) {
            t = t.substring(4);
        } else if (t.startsWith("00222")) {
            t = t.substring(5);
        }
        if (!TELEPHONE.matcher(t).matches()) {
            throw new InscriptionInvalideException(InscriptionInvalideException.TELEPHONE_INVALIDE);
        }
        return t;
    }

    private static String texte(String saisie, int longueurMax, String code) {
        String s = saisie == null ? "" : saisie.strip().replaceAll("\\s+", " ");
        if (s.isEmpty() || s.length() > longueurMax) {
            throw new InscriptionInvalideException(code);
        }
        return s;
    }

    /** Aucune donnée personnelle : ce résumé peut finir dans les journaux. */
    @Override
    public String toString() {
        return "Inscription[" + id + ", " + typeContrat + ", " + trancheSalaire + "]";
    }
}
