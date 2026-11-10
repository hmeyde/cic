package com.cic.commun.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Montant en ouguiyas (MRU), toujours arrondi à l'ouguiya entière (HALF_UP).
 *
 * <p>Un montant est positif ou nul : le sens d'un mouvement (débit, crédit, écriture inverse)
 * est porté par l'objet qui l'utilise, pas par le signe du montant.
 * Immuable ; deux montants égaux ont la même valeur, quelle que soit l'échelle d'entrée.
 */
public record Montant(BigDecimal valeur) implements Comparable<Montant> {

    public static final String DEVISE = "MRU";
    public static final RoundingMode ARRONDI = RoundingMode.HALF_UP;
    public static final Montant ZERO = new Montant(BigDecimal.ZERO);

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    public Montant {
        Objects.requireNonNull(valeur, "valeur");
        valeur = valeur.setScale(0, ARRONDI);
        if (valeur.signum() < 0) {
            throw new IllegalArgumentException("Un montant ne peut pas être négatif : " + valeur);
        }
    }

    public static Montant de(long ouguiyas) {
        return new Montant(BigDecimal.valueOf(ouguiyas));
    }

    public static Montant de(BigDecimal valeur) {
        return new Montant(valeur);
    }

    /** Lit un montant écrit avec un point décimal, par exemple {@code "20800"} ou {@code "1250.50"}. */
    public static Montant de(String valeur) {
        Objects.requireNonNull(valeur, "valeur");
        return new Montant(new BigDecimal(valeur.strip()));
    }

    public Montant plus(Montant autre) {
        return new Montant(valeur.add(autre.valeur));
    }

    /** @throws IllegalArgumentException si le résultat serait négatif */
    public Montant moins(Montant autre) {
        return new Montant(valeur.subtract(autre.valeur));
    }

    /** Multiplie puis arrondit une seule fois à l'ouguiya. */
    public Montant fois(BigDecimal facteur) {
        Objects.requireNonNull(facteur, "facteur");
        return new Montant(valeur.multiply(facteur));
    }

    public Montant fois(long facteur) {
        return new Montant(valeur.multiply(BigDecimal.valueOf(facteur)));
    }

    /**
     * Applique un taux exprimé en pourcentage : {@code de(20000).pourcentage(new BigDecimal("8"))}
     * vaut 1 600 MRU. Arrondi une seule fois à l'ouguiya.
     */
    public Montant pourcentage(BigDecimal taux) {
        Objects.requireNonNull(taux, "taux");
        return new Montant(valeur.multiply(taux).divide(CENT, 0, ARRONDI));
    }

    /**
     * Répartit le montant en {@code nombre} parts : chaque part vaut le quotient arrondi
     * à l'ouguiya (HALF_UP) et la dernière absorbe l'écart, de sorte que la somme des parts
     * est exactement égale au montant.
     *
     * @throws IllegalArgumentException si {@code nombre} n'est pas strictement positif ou si
     *         le montant est trop petit pour que la dernière part reste positive
     */
    public List<Montant> repartir(int nombre) {
        if (nombre <= 0) {
            throw new IllegalArgumentException("Le nombre de parts doit être positif : " + nombre);
        }
        BigDecimal part = valeur.divide(BigDecimal.valueOf(nombre), 0, ARRONDI);
        BigDecimal derniere = valeur.subtract(part.multiply(BigDecimal.valueOf(nombre - 1L)));
        if (derniere.signum() < 0) {
            throw new IllegalArgumentException(
                    "Montant " + this + " trop petit pour être réparti en " + nombre + " parts");
        }
        List<Montant> parts = new ArrayList<>(nombre);
        Montant montantPart = new Montant(part);
        for (int i = 0; i < nombre - 1; i++) {
            parts.add(montantPart);
        }
        parts.add(new Montant(derniere));
        return Collections.unmodifiableList(parts);
    }

    public boolean estZero() {
        return valeur.signum() == 0;
    }

    public boolean estSuperieurA(Montant autre) {
        return compareTo(autre) > 0;
    }

    public boolean estSuperieurOuEgalA(Montant autre) {
        return compareTo(autre) >= 0;
    }

    public boolean estInferieurA(Montant autre) {
        return compareTo(autre) < 0;
    }

    public boolean estInferieurOuEgalA(Montant autre) {
        return compareTo(autre) <= 0;
    }

    @Override
    public int compareTo(Montant autre) {
        return valeur.compareTo(autre.valeur);
    }

    /** Représentation technique, par exemple {@code "20800 MRU"} ; l'affichage localisé se fait dans la vue. */
    @Override
    public String toString() {
        return valeur.toPlainString() + " " + DEVISE;
    }
}
