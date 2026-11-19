package com.seheli.pilotage.adapter.in.web;

import static com.seheli.pilotage.adapter.in.web.Courbe.arrondi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.seheli.commun.domain.Montant;
import com.seheli.pilotage.domain.Concentration;

/**
 * Géométrie des barres horizontales de concentration de l'encours (part de chaque employeur ou
 * fournisseur), dessinées en SVG par le gabarit. Au plus {@value #LIGNES_MAX} barres : au-delà, les plus
 * petits groupes sont regroupés dans une dernière barre « Autres ». La ligne verticale marque le seuil.
 *
 * <p>En arabe ({@code rtl}), les barres partent de la droite. Libellés et valeurs sont ancrés en
 * {@code start} : ils suivent le sens de la page.
 */
public record Barres(List<Ligne> lignes, BigDecimal seuilPct, double xSeuil, BigDecimal echellePct, int hauteur,
        boolean rtl) {

    static final int LIGNES_MAX = 5;
    static final int LARGEUR = 640;
    /** Place gardée au bout de la plus longue barre pour sa valeur. */
    private static final int MARGE_VALEUR = 150;
    private static final int HAUTEUR_LIGNE = 48;
    private static final int EPAISSEUR = 16;
    private static final int RAYON = 4;
    private static final int MARGE_BAS = 28;
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    static Barres de(List<Concentration> repartition, BigDecimal seuilPct, boolean rtl) {
        Objects.requireNonNull(repartition, "repartition");
        Objects.requireNonNull(seuilPct, "seuilPct");
        Montant total = repartition.stream().map(Concentration::montant).reduce(Montant.ZERO, Montant::plus);

        List<Concentration> retenues = repartition.size() <= LIGNES_MAX ? repartition
                : repartition.subList(0, LIGNES_MAX - 1);
        List<Concentration> autres = repartition.subList(retenues.size(), repartition.size());

        BigDecimal plusForte = repartition.isEmpty() ? BigDecimal.ZERO : repartition.getFirst().partPct();
        BigDecimal echelle = plusForte.max(seuilPct).add(BigDecimal.valueOf(5))
                .divide(BigDecimal.TEN, 0, RoundingMode.CEILING).multiply(BigDecimal.TEN).min(CENT);
        double largeurUtile = LARGEUR - MARGE_VALEUR;

        List<Ligne> lignes = new ArrayList<>();
        int rang = 0;
        for (Concentration c : retenues) {
            lignes.add(ligne(rang++, c.libelle().orElse(null), 0, c.montant(), c.partPct(),
                    c.partPct().compareTo(seuilPct) > 0, echelle, largeurUtile, rtl));
        }
        if (!autres.isEmpty()) {
            Montant montant = autres.stream().map(Concentration::montant).reduce(Montant.ZERO, Montant::plus);
            BigDecimal part = montant.valeur().multiply(CENT).divide(total.valeur(), 1, RoundingMode.HALF_UP);
            lignes.add(ligne(rang, null, autres.size(), montant, part, false, echelle, largeurUtile, rtl));
        }
        double xSeuil = x(largeurUtile * seuilPct.doubleValue() / echelle.doubleValue(), rtl);
        return new Barres(lignes, seuilPct.stripTrailingZeros(), arrondi(xSeuil), echelle,
                Math.max(1, lignes.size()) * HAUTEUR_LIGNE + MARGE_BAS, rtl);
    }

    private static Ligne ligne(int rang, String libelle, int nombreAutres, Montant montant, BigDecimal partPct,
            boolean depasse, BigDecimal echelle, double largeurUtile, boolean rtl) {
        double yLibelle = rang * HAUTEUR_LIGNE + 16;
        double yBarre = yLibelle + 8;
        double longueur = Math.max(2, largeurUtile * Math.min(partPct.doubleValue() / echelle.doubleValue(), 1));
        double xValeur = x(longueur + 8, rtl);
        return new Ligne(libelle, nombreAutres, montant, partPct, depasse, yLibelle, yBarre + EPAISSEUR / 2.0 + 5,
                forme(arrondi(longueur), yBarre, rtl), arrondi(xValeur));
    }

    /** Barre à bout arrondi côté valeur, à angle droit sur la ligne de base. */
    private static String forme(double longueur, double y, boolean rtl) {
        double r = Math.min(RAYON, longueur / 2);
        double droit = longueur - r;
        String sens = rtl ? "-" : "";
        String balayage = rtl ? "0" : "1";
        double depart = rtl ? LARGEUR : 0;
        return "M" + depart + ' ' + y
                + " h" + sens + arrondi(droit)
                + " a" + r + ' ' + r + " 0 0 " + balayage + ' ' + sens + r + ' ' + r
                + " v" + (EPAISSEUR - 2 * r)
                + " a" + r + ' ' + r + " 0 0 " + balayage + ' ' + (rtl ? "" : "-") + r + ' ' + r
                + " h" + (rtl ? "" : "-") + arrondi(droit) + " Z";
    }

    private static double x(double distance, boolean rtl) {
        return rtl ? LARGEUR - distance : distance;
    }

    public String viewBox() {
        return "0 0 " + LARGEUR + " " + hauteur;
    }

    /** Abscisse de départ des libellés et des barres. */
    public double xDepart() {
        return rtl ? LARGEUR : 0;
    }

    /** Haut et bas de la ligne de seuil, et ordonnée de son libellé. */
    public double ySeuilBas() {
        return hauteur - MARGE_BAS + 4;
    }

    public double yLibelleSeuil() {
        return hauteur - 8;
    }

    /**
     * @param libelle      nom de l'employeur ou du fournisseur ; {@code null} pour « non renseigné » ou « Autres »
     * @param nombreAutres nombre de groupes regroupés dans « Autres » ; 0 pour une ligne ordinaire
     * @param depasse      la part dépasse le seuil
     * @param yValeur      ordonnée de la valeur écrite au bout de la barre
     * @param forme        attribut {@code d} de la barre
     */
    public record Ligne(String libelle, int nombreAutres, Montant montant, BigDecimal partPct, boolean depasse,
            double yLibelle, double yValeur, String forme, double xValeur) {

        public boolean autres() {
            return nombreAutres > 0;
        }
    }
}
