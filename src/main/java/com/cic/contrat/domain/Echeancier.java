package com.cic.contrat.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cic.commun.domain.Fraction;
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
     * @throws IllegalArgumentException si la durée sort des bornes, si le jour de paie est invalide
     *         ou si le reste est trop petit pour être réparti
     * @throws FinContratDepasseeException si la dernière échéance ne tombe pas avant la fin du CDD
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
                throw new FinContratDepasseeException(derniere, fin);
            }
        });
        return new Echeancier(echeances);
    }

    /**
     * Contrôle d'endettement : la mensualité la plus élevée de cet échéancier, ajoutée aux
     * mensualités CIC déjà dues par le client, ne doit pas dépasser {@code partMax} de son
     * salaire net. Comparaison exacte, sans arrondi (1/3 n'est pas 33 %).
     *
     * @param salaireNet            salaire net mensuel du client
     * @param mensualitesCicEnCours total des mensualités CIC qu'il rembourse déjà (zéro sinon)
     * @param partMax               part maximale du salaire ({@code endettement.max.fraction})
     * @throws EndettementDepasseException si le plafond est dépassé
     */
    public void verifierEndettement(Montant salaireNet, Montant mensualitesCicEnCours, Fraction partMax) {
        Objects.requireNonNull(salaireNet, "salaireNet");
        Objects.requireNonNull(mensualitesCicEnCours, "mensualitesCicEnCours");
        Objects.requireNonNull(partMax, "partMax");
        Montant total = mensualitesCicEnCours.plus(mensualiteMaximale());
        if (!partMax.admet(total, salaireNet)) {
            throw new EndettementDepasseException(total, salaireNet, partMax);
        }
    }

    /** Mensualité la plus élevée : la dernière peut dépasser les autres quand elle absorbe l'écart. */
    public Montant mensualiteMaximale() {
        return echeances.stream().map(EcheancePrevue::montant).max(Montant::compareTo).orElseThrow();
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
