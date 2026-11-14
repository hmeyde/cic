package com.seheli.demande.domain;

import static com.seheli.demande.domain.CritereScore.ANCIENNETE;
import static com.seheli.demande.domain.CritereScore.APPORT_SUPPLEMENTAIRE;
import static com.seheli.demande.domain.CritereScore.GARANT;
import static com.seheli.demande.domain.CritereScore.HISTORIQUE_CIC;
import static com.seheli.demande.domain.CritereScore.MODE_REMBOURSEMENT;
import static com.seheli.demande.domain.CritereScore.PART_SALAIRE;
import static com.seheli.demande.domain.CritereScore.TYPE_CONTRAT;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.seheli.commun.domain.Montant;

/**
 * Note sur 100 de la grille de score v1 (CLAUDE.md) :
 * <ul>
 *   <li>type de contrat (25) : fonctionnaire ou CDI 25 ; CDD finissant après la dernière échéance 15 ;</li>
 *   <li>ancienneté chez l'employeur (15) : plus de 2 ans 15 ; de 1 à 2 ans 10 ; moins d'un an 5 ;</li>
 *   <li>mode de remboursement (15) : retenue employeur ou domiciliation BMCI 15 ; virement permanent 8 ;</li>
 *   <li>part du salaire (15) : moins de 20 % 15 ; de 20 % au plafond d'endettement (1/3) 8 ;</li>
 *   <li>apport au-delà du minimum (10) : proportionnel à l'apport supplémentaire ;</li>
 *   <li>garant (10) ;</li>
 *   <li>historique chez Seheli (10) : 5 points par plan remboursé à temps.</li>
 * </ul>
 * Les points sont entiers. Le barème est figé dans le code, une nouvelle grille aura une
 * nouvelle version ; les seuils de lecture viennent des paramètres.
 */
public final class GrilleScoreV1 {

    public static final String VERSION = "v1";

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final BigDecimal PART_SALAIRE_BASSE_PCT = BigDecimal.valueOf(20);
    private static final int POINTS_PAR_PLAN = 5;

    private GrilleScoreV1() {
    }

    /**
     * @param date date d'évaluation (issue de la {@code Clock} de l'application)
     */
    public static NoteScore noter(DossierScore dossier, ReglesScore regles, LocalDate date) {
        Objects.requireNonNull(dossier, "dossier");
        Objects.requireNonNull(regles, "regles");
        Objects.requireNonNull(date, "date");
        List<ResultatCritere> criteres = List.of(
                typeContrat(dossier),
                anciennete(dossier, date),
                modeRemboursement(dossier),
                partSalaire(dossier, regles),
                apportSupplementaire(dossier),
                garant(dossier),
                historique(dossier));
        int note = criteres.stream().mapToInt(ResultatCritere::points).sum();
        return new NoteScore(VERSION, criteres, Recommandation.selon(note, regles));
    }

    static ResultatCritere typeContrat(DossierScore d) {
        return switch (d.typeContrat()) {
            case FONCTIONNAIRE -> new ResultatCritere(TYPE_CONTRAT, 25, "Fonctionnaire");
            case CDI -> new ResultatCritere(TYPE_CONTRAT, 25, "CDI");
            case CDD -> {
                LocalDate fin = d.finContrat().orElseThrow();
                yield fin.isAfter(d.derniereEcheance())
                        ? new ResultatCritere(TYPE_CONTRAT, 15, "CDD jusqu'au " + fin
                                + ", après la dernière échéance du " + d.derniereEcheance())
                        : new ResultatCritere(TYPE_CONTRAT, 0, "CDD jusqu'au " + fin
                                + ", pas après la dernière échéance du " + d.derniereEcheance());
            }
        };
    }

    static ResultatCritere anciennete(DossierScore d, LocalDate date) {
        LocalDate debut = d.debutContrat();
        if (debut.isAfter(date)) {
            return new ResultatCritere(ANCIENNETE, 0, "Contrat pas encore commencé (début le " + debut + ")");
        }
        if (debut.plusYears(2).isBefore(date)) {
            return new ResultatCritere(ANCIENNETE, 15, "Plus de 2 ans chez l'employeur (depuis le " + debut + ")");
        }
        if (!debut.plusYears(1).isAfter(date)) {
            return new ResultatCritere(ANCIENNETE, 10, "De 1 à 2 ans chez l'employeur (depuis le " + debut + ")");
        }
        return new ResultatCritere(ANCIENNETE, 5, "Moins d'un an chez l'employeur (depuis le " + debut + ")");
    }

    static ResultatCritere modeRemboursement(DossierScore d) {
        return switch (d.modeRemboursement()) {
            case RETENUE_EMPLOYEUR -> new ResultatCritere(MODE_REMBOURSEMENT, 15, "Retenue par l'employeur");
            case DOMICILIATION_BMCI -> new ResultatCritere(MODE_REMBOURSEMENT, 15, "Salaire domicilié à la BMCI");
            case VIREMENT_PERMANENT -> new ResultatCritere(MODE_REMBOURSEMENT, 8, "Virement permanent seul");
        };
    }

    static ResultatCritere partSalaire(DossierScore d, ReglesScore r) {
        Montant total = d.mensualitesCicEnCours().plus(d.nouvelleMensualite());
        if (d.salaireNet().estZero()) {
            return new ResultatCritere(PART_SALAIRE, 0, "Salaire net nul");
        }
        BigDecimal part = total.valeur().multiply(CENT).divide(d.salaireNet().valeur(), 1, RoundingMode.HALF_UP);
        String detail = "mensualités Seheli " + total + " pour un salaire net de " + d.salaireNet()
                + " (" + part.toPlainString() + " %)";
        // Comparaisons exactes : total × 100 < 20 × salaire, puis total ≤ salaire × plafond.
        if (total.valeur().multiply(CENT).compareTo(d.salaireNet().valeur().multiply(PART_SALAIRE_BASSE_PCT)) < 0) {
            return new ResultatCritere(PART_SALAIRE, 15, "Moins de 20 % du salaire : " + detail);
        }
        if (r.endettementMax().admet(total, d.salaireNet())) {
            return new ResultatCritere(PART_SALAIRE, 8, "De 20 % au plafond de " + r.endettementMax()
                    + " du salaire : " + detail);
        }
        return new ResultatCritere(PART_SALAIRE, 0, "Au-delà du plafond de " + r.endettementMax()
                + " du salaire : " + detail);
    }

    /**
     * Points = 10 × (apport − apport minimum) / (prix de vente − apport minimum), arrondi à
     * l'entier inférieur : 0 au minimum, 10 pour un apport égal au prix de vente.
     */
    static ResultatCritere apportSupplementaire(DossierScore d) {
        Montant apport = d.apport();
        Montant minimum = d.apportMinimum();
        if (apport.estInferieurA(minimum)) {
            return new ResultatCritere(APPORT_SUPPLEMENTAIRE, 0,
                    "Apport de " + apport + " inférieur au minimum de " + minimum);
        }
        if (apport.estSuperieurOuEgalA(d.prixVente())) {
            return new ResultatCritere(APPORT_SUPPLEMENTAIRE, APPORT_SUPPLEMENTAIRE.pointsMax(),
                    "Apport de " + apport + " couvrant le prix de vente de " + d.prixVente());
        }
        Montant supplement = apport.moins(minimum);
        Montant plage = d.prixVente().moins(minimum);
        int points = supplement.valeur().multiply(BigDecimal.valueOf(APPORT_SUPPLEMENTAIRE.pointsMax()))
                .divide(plage.valeur(), 0, RoundingMode.FLOOR).intValueExact();
        String raison = supplement.estZero()
                ? "Apport minimum de " + minimum
                : "Apport de " + apport + ", soit " + supplement + " au-delà du minimum de " + minimum
                        + " sur " + plage + " finançables";
        return new ResultatCritere(APPORT_SUPPLEMENTAIRE, points, raison);
    }

    static ResultatCritere garant(DossierScore d) {
        return d.garant()
                ? new ResultatCritere(GARANT, 10, "Garant présenté")
                : new ResultatCritere(GARANT, 0, "Aucun garant");
    }

    static ResultatCritere historique(DossierScore d) {
        int n = d.plansRembourseATemps();
        int points = Math.min(HISTORIQUE_CIC.pointsMax(), n * POINTS_PAR_PLAN);
        String raison = switch (n) {
            case 0 -> "Aucun plan Seheli déjà remboursé à temps";
            case 1 -> "1 plan Seheli remboursé à temps";
            default -> n + " plans Seheli remboursés à temps";
        };
        return new ResultatCritere(HISTORIQUE_CIC, points, raison);
    }
}
