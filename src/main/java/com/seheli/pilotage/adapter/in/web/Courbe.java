package com.seheli.pilotage.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Géométrie d'une courbe mensuelle à une seule série, dessinée en SVG par le gabarit (aucun script).
 * Les valeurs restent des {@link BigDecimal} (montants ou pourcentages) ; seules les coordonnées en
 * pixels sont des nombres à virgule, arrondies au dixième.
 *
 * <p>En arabe ({@code rtl}), l'axe du temps va de droite à gauche et les graduations passent à droite,
 * comme le reste de la page. Les textes héritent du sens de la page : le gabarit les ancre en
 * {@code start} / {@code end}, que le navigateur inverse en arabe.
 *
 * @param points      un point par mois, du plus ancien au plus récent
 * @param graduations graduations de l'axe vertical, de zéro au maximum
 * @param seuil       ligne de référence (seuil de paramètre) ; {@code null} s'il n'y en a pas
 * @param trace       attribut {@code d} de la ligne, interrompue là où un mois n'a pas de valeur
 * @param aire        attribut {@code d} du lavis sous la ligne ; vide si la ligne est interrompue
 * @param dernier     dernier point qui a une valeur, étiqueté directement ; {@code null} s'il n'y en a pas
 */
public record Courbe(List<Point> points, List<Graduation> graduations, Graduation seuil, String trace, String aire,
        Point dernier, boolean rtl) {

    static final int LARGEUR = 640;
    static final int HAUTEUR = 260;
    static final int MARGE_AXE = 72;
    static final int MARGE_OPPOSEE = 28;
    static final int MARGE_HAUT = 28;
    static final int MARGE_BAS = 40;
    private static final int INTERVALLES = 4;

    /**
     * @param mois     premier jour de chaque mois
     * @param valeurs  une valeur par mois ({@code null} : pas de valeur ce mois-là)
     * @param seuil    ligne de référence, {@code null} si aucune
     * @param maximum  maximum imposé de l'axe (100 pour un pourcentage) ; {@code null} pour l'ajuster aux valeurs
     */
    static Courbe de(List<LocalDate> mois, List<BigDecimal> valeurs, BigDecimal seuil, BigDecimal maximum,
            boolean rtl) {
        Objects.requireNonNull(mois, "mois");
        Objects.requireNonNull(valeurs, "valeurs");
        if (mois.size() != valeurs.size() || mois.size() < 2) {
            throw new IllegalArgumentException("Il faut au moins deux mois, chacun avec sa valeur");
        }
        BigDecimal haut = maximum != null ? maximum : arrondiHaut(plusGrand(valeurs, seuil));
        BigDecimal pas = haut.divide(BigDecimal.valueOf(INTERVALLES));

        List<Graduation> graduations = new ArrayList<>();
        for (int i = 0; i <= INTERVALLES; i++) {
            BigDecimal v = pas.multiply(BigDecimal.valueOf(i));
            graduations.add(new Graduation(y(v, haut), v.stripTrailingZeros()));
        }

        double debut = MARGE_AXE;
        double largeurUtile = LARGEUR - MARGE_AXE - MARGE_OPPOSEE;
        List<Point> points = new ArrayList<>();
        StringBuilder trace = new StringBuilder();
        boolean interrompue = false;
        boolean enCours = false;
        Point dernier = null;
        for (int i = 0; i < mois.size(); i++) {
            double x = miroir(debut + largeurUtile * i / (mois.size() - 1), rtl);
            BigDecimal v = valeurs.get(i);
            Point p = new Point(arrondi(x), v == null ? y(BigDecimal.ZERO, haut) : y(v, haut), v, mois.get(i));
            points.add(p);
            if (v == null) {
                interrompue = true;
                enCours = false;
                continue;
            }
            trace.append(enCours ? " L" : (trace.isEmpty() ? "M" : " M")).append(p.x()).append(' ').append(p.y());
            enCours = true;
            dernier = p;
        }
        String aire = "";
        if (!interrompue) {
            double base = y(BigDecimal.ZERO, haut);
            aire = trace + " L" + points.getLast().x() + ' ' + base + " L" + points.getFirst().x() + ' ' + base + " Z";
        }
        Graduation ligneSeuil = seuil == null ? null : new Graduation(y(seuil, haut), seuil.stripTrailingZeros());
        return new Courbe(points, graduations, ligneSeuil, trace.toString(), aire, dernier, rtl);
    }

    /** Abscisse du début de l'axe horizontal (côté des graduations). */
    public double xAxe() {
        return miroir(MARGE_AXE, rtl);
    }

    /** Abscisse de l'autre extrémité de l'axe horizontal. */
    public double xFin() {
        return miroir(LARGEUR - MARGE_OPPOSEE, rtl);
    }

    /** Abscisse des libellés de graduation, juste avant l'axe. */
    public double xLibelles() {
        return miroir(MARGE_AXE - 8, rtl);
    }

    public double yBase() {
        return y(BigDecimal.ZERO, BigDecimal.ONE);
    }

    public double yMois() {
        return HAUTEUR - MARGE_BAS + 22;
    }

    public String viewBox() {
        return "0 0 " + LARGEUR + " " + HAUTEUR;
    }

    private static double y(BigDecimal v, BigDecimal haut) {
        double hauteurUtile = HAUTEUR - MARGE_HAUT - MARGE_BAS;
        double part = haut.signum() == 0 ? 0 : v.doubleValue() / haut.doubleValue();
        return arrondi(MARGE_HAUT + hauteurUtile * (1 - Math.min(part, 1)));
    }

    private static double miroir(double x, boolean rtl) {
        return rtl ? LARGEUR - x : x;
    }

    static double arrondi(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static BigDecimal plusGrand(List<BigDecimal> valeurs, BigDecimal seuil) {
        BigDecimal max = seuil == null ? BigDecimal.ZERO : seuil;
        for (BigDecimal v : valeurs) {
            if (v != null && v.compareTo(max) > 0) {
                max = v;
            }
        }
        return max;
    }

    /**
     * Plus petit maximum « rond » au moins égal à {@code v} et divisible en quatre graduations rondes :
     * 4 × (1, 2, 2,5 ou 5) × 10^n. Zéro donne 4 (axe 0–4, courbe à plat).
     */
    static BigDecimal arrondiHaut(BigDecimal v) {
        if (v.signum() <= 0) {
            return BigDecimal.valueOf(INTERVALLES);
        }
        BigDecimal brut = v.divide(BigDecimal.valueOf(INTERVALLES), 6, java.math.RoundingMode.CEILING);
        BigDecimal puissance = BigDecimal.ONE;
        while (puissance.multiply(BigDecimal.TEN).compareTo(brut) <= 0) {
            puissance = puissance.multiply(BigDecimal.TEN);
        }
        while (puissance.compareTo(brut) > 0 && puissance.compareTo(BigDecimal.ONE) > 0) {
            puissance = puissance.divide(BigDecimal.TEN);
        }
        for (String facteur : new String[] {"1", "2", "2.5", "5", "10"}) {
            BigDecimal pas = puissance.multiply(new BigDecimal(facteur));
            if (pas.compareTo(brut) >= 0) {
                return pas.multiply(BigDecimal.valueOf(INTERVALLES)).stripTrailingZeros();
            }
        }
        throw new IllegalStateException("inatteignable");
    }

    /** @param valeur {@code null} si le mois n'a pas de valeur (aucun marqueur n'est dessiné) */
    public record Point(double x, double y, BigDecimal valeur, LocalDate mois) {

        public boolean aValeur() {
            return valeur != null;
        }
    }

    public record Graduation(double y, BigDecimal valeur) {
    }
}
