package com.cic.contrat.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cic.commun.domain.Montant;

/**
 * Échéancier d'un contrat : mensualités égales arrondies à l'ouguiya (HALF_UP), la dernière
 * absorbant l'écart, prélevées au jour de paie du client.
 *
 * <p>Règles de dates :
 * <ul>
 *   <li>la première échéance tombe au premier jour de paie strictement postérieur à la date
 *       de départ (date de signature ou de remise du produit) ;</li>
 *   <li>les suivantes tombent au jour de paie des mois suivants ;</li>
 *   <li>si le mois est plus court que le jour de paie (par exemple 31 en février), l'échéance
 *       tombe le dernier jour du mois ;</li>
 *   <li>pour un CDD, la dernière échéance doit tomber strictement avant la fin du contrat.</li>
 * </ul>
 * Immuable : l'échéancier est fixé à la signature.
 */
public record Echeancier(List<EcheancePrevue> echeances) {

    public Echeancier {
        Objects.requireNonNull(echeances, "echeances");
        if (echeances.isEmpty()) {
            throw new IllegalArgumentException("Un échéancier contient au moins une échéance");
        }
        echeances = List.copyOf(echeances);
    }

    /**
     * @param resteAFinancer  prix de vente − apport
     * @param nombreMois      nombre de mensualités demandé
     * @param duree           bornes autorisées du nombre de mensualités
     * @param dateDepart      date à partir de laquelle court l'échéancier
     * @param jourPaie        jour du mois où le salaire est versé (1 à 31)
     * @param finContrat      date de fin du contrat de travail (CDD) ; vide pour un CDI ou un fonctionnaire
     * @throws IllegalArgumentException si la durée sort des bornes, si le jour de paie est invalide,
     *         si le reste est trop petit pour être réparti ou si la dernière échéance ne tombe
     *         pas avant la fin du CDD
     */
    public static Echeancier etablir(Montant resteAFinancer, int nombreMois, DureeAutorisee duree,
            LocalDate dateDepart, int jourPaie, Optional<LocalDate> finContrat) {
        Objects.requireNonNull(resteAFinancer, "resteAFinancer");
        Objects.requireNonNull(duree, "duree");
        Objects.requireNonNull(dateDepart, "dateDepart");
        Objects.requireNonNull(finContrat, "finContrat");
        if (!duree.autorise(nombreMois)) {
            throw new IllegalArgumentException("Durée de " + nombreMois + " mois hors bornes ["
                    + duree.minMois() + " ; " + duree.maxMois() + "]");
        }
        if (jourPaie < 1 || jourPaie > 31) {
            throw new IllegalArgumentException("Jour de paie invalide : " + jourPaie);
        }
        if (resteAFinancer.estZero()) {
            throw new IllegalArgumentException("Rien à financer : l'échéancier serait vide");
        }

        List<Montant> montants = resteAFinancer.repartir(nombreMois);
        if (montants.getLast().estZero()) {
            throw new IllegalArgumentException(
                    "Reste " + resteAFinancer + " trop petit pour " + nombreMois + " mensualités");
        }

        YearMonth mois = YearMonth.from(dateDepart);
        if (!jourDePaie(mois, jourPaie).isAfter(dateDepart)) {
            mois = mois.plusMonths(1);
        }
        List<EcheancePrevue> echeances = new ArrayList<>(nombreMois);
        for (int i = 0; i < nombreMois; i++) {
            echeances.add(new EcheancePrevue(i + 1, jourDePaie(mois.plusMonths(i), jourPaie), montants.get(i)));
        }

        LocalDate derniere = echeances.getLast().date();
        finContrat.ifPresent(fin -> {
            if (!derniere.isBefore(fin)) {
                throw new IllegalArgumentException("La dernière échéance (" + derniere
                        + ") doit tomber avant la fin du contrat de travail (" + fin + ")");
            }
        });
        return new Echeancier(echeances);
    }

    private static LocalDate jourDePaie(YearMonth mois, int jourPaie) {
        return mois.atDay(Math.min(jourPaie, mois.lengthOfMonth()));
    }

    public int nombre() {
        return echeances.size();
    }

    public Montant total() {
        return echeances.stream().map(EcheancePrevue::montant).reduce(Montant.ZERO, Montant::plus);
    }

    /** Mensualité courante (celle des premières échéances), utilisée pour le taux d'endettement. */
    public Montant mensualite() {
        return echeances.getFirst().montant();
    }

    public EcheancePrevue premiere() {
        return echeances.getFirst();
    }

    public EcheancePrevue derniere() {
        return echeances.getLast();
    }
}
