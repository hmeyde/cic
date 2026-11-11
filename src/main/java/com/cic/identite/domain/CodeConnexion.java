package com.cic.identite.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Code de connexion à usage unique envoyé à un téléphone.
 *
 * <p>Le code est rattaché au téléphone et non au client : un nouveau client reçoit aussi un code, qui
 * prouve que le numéro est à lui avant la création de son compte.
 * Un téléphone a au plus un code en cours : en émettre un nouveau remplace le précédent.
 * Le code n'est connu qu'au moment de l'envoi ; seul son hachage est conservé.
 * Il est valable pendant une durée limitée, pour un nombre limité d'essais, et une seule fois.
 */
public final class CodeConnexion {

    private final String telephone;
    private final String codeHash;
    private final Instant emisLe;
    private final Instant expireLe;
    private final int essaisMax;
    private int essais;
    private boolean utilise;

    private CodeConnexion(String telephone, String codeHash, Instant emisLe, Instant expireLe, int essaisMax,
            int essais, boolean utilise) {
        this.telephone = Objects.requireNonNull(telephone, "telephone");
        this.codeHash = Objects.requireNonNull(codeHash, "codeHash");
        this.emisLe = Objects.requireNonNull(emisLe, "emisLe");
        this.expireLe = Objects.requireNonNull(expireLe, "expireLe");
        if (!expireLe.isAfter(emisLe)) {
            throw new IllegalArgumentException("Le code doit expirer après son émission");
        }
        if (essaisMax < 1 || essais < 0 || essais > essaisMax) {
            throw new IllegalArgumentException("Nombre d'essais incohérent");
        }
        this.essaisMax = essaisMax;
        this.essais = essais;
        this.utilise = utilise;
    }

    /** Nouveau code émis à {@code maintenant}, valable pendant {@code validite} et {@code essaisMax} essais. */
    public static CodeConnexion emettre(String telephone, String codeHash, Instant maintenant, Duration validite,
            int essaisMax) {
        Objects.requireNonNull(validite, "validite");
        return new CodeConnexion(telephone, codeHash, maintenant, maintenant.plus(validite), essaisMax, 0, false);
    }

    /** Reconstitution depuis le stockage. */
    public static CodeConnexion reconstituer(String telephone, String codeHash, Instant emisLe, Instant expireLe,
            int essaisMax, int essais, boolean utilise) {
        return new CodeConnexion(telephone, codeHash, emisLe, expireLe, essaisMax, essais, utilise);
    }

    /**
     * Vérifie le code saisi. Chaque essai sur un code encore valable est décompté, qu'il réussisse ou non.
     *
     * @param correspond compare le code saisi au hachage conservé (par exemple BCrypt)
     */
    public Verification verifier(String codeSaisi, Instant maintenant, BiPredicate<String, String> correspond) {
        Objects.requireNonNull(maintenant, "maintenant");
        if (utilise) {
            return Verification.DEJA_UTILISE;
        }
        if (estExpireA(maintenant)) {
            return Verification.EXPIRE;
        }
        if (essais >= essaisMax) {
            return Verification.ESSAIS_EPUISES;
        }
        essais++;
        if (codeSaisi != null && correspond.test(codeSaisi, codeHash)) {
            utilise = true;
            return Verification.VALIDE;
        }
        return essais >= essaisMax ? Verification.ESSAIS_EPUISES : Verification.INCORRECT;
    }

    /** Vrai si le code ne peut plus servir : utilisé, expiré ou essais épuisés. */
    public boolean estInutilisableA(Instant maintenant) {
        return utilise || estExpireA(maintenant) || essais >= essaisMax;
    }

    private boolean estExpireA(Instant maintenant) {
        return !maintenant.isBefore(expireLe);
    }

    public int essaisRestants() {
        return essaisMax - essais;
    }

    public String telephone() {
        return telephone;
    }

    public String codeHash() {
        return codeHash;
    }

    public Instant emisLe() {
        return emisLe;
    }

    public Instant expireLe() {
        return expireLe;
    }

    public int essaisMax() {
        return essaisMax;
    }

    public int essais() {
        return essais;
    }

    public boolean estUtilise() {
        return utilise;
    }

    /** Résultat de la vérification d'un code saisi. */
    public enum Verification {
        VALIDE,
        INCORRECT,
        EXPIRE,
        ESSAIS_EPUISES,
        DEJA_UTILISE
    }

    /** Sans le hachage du code ni le téléphone (donnée personnelle). */
    @Override
    public String toString() {
        return "CodeConnexion[expireLe=" + expireLe + ", essais=" + essais + "/" + essaisMax
                + ", utilise=" + utilise + "]";
    }
}
