package com.seheli.commun.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Fraction exacte et positive, par exemple 1/3 pour la part maximale du salaire consacrée
 * aux mensualités. Évite l'approximation d'un pourcentage (33 % n'est pas 1/3).
 */
public record Fraction(long numerateur, long denominateur) {

    public Fraction {
        if (numerateur < 0 || denominateur <= 0) {
            throw new IllegalArgumentException("Fraction invalide : " + numerateur + "/" + denominateur);
        }
    }

    /** Lit une fraction écrite {@code "a/b"}, par exemple {@code "1/3"}. */
    public static Fraction de(String texte) {
        Objects.requireNonNull(texte, "texte");
        String[] parties = texte.strip().split("/", -1);
        if (parties.length != 2) {
            throw new IllegalArgumentException("Fraction attendue au format a/b : " + texte);
        }
        return new Fraction(Long.parseLong(parties[0].strip()), Long.parseLong(parties[1].strip()));
    }

    /** Vrai si {@code part ≤ total × fraction}, comparaison exacte sans arrondi. */
    public boolean admet(Montant part, Montant total) {
        BigDecimal gauche = part.valeur().multiply(BigDecimal.valueOf(denominateur));
        BigDecimal droite = total.valeur().multiply(BigDecimal.valueOf(numerateur));
        return gauche.compareTo(droite) <= 0;
    }

    /** Valeur en pourcentage arrondie à {@code decimales} décimales (HALF_UP), pour l'affichage. */
    public BigDecimal enPourcentage(int decimales) {
        return BigDecimal.valueOf(numerateur).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominateur), decimales, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return numerateur + "/" + denominateur;
    }
}
