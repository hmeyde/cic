package com.seheli.contrat.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.seheli.commun.domain.Fraction;
import com.seheli.commun.domain.Montant;

/**
 * Simulation présentée au client : apport choisi, échéancier daté au jour de paie du client,
 * décomposition du prix total et alertes (endettement, fin de CDD).
 *
 * <p>Le prix total est le prix de vente : prix public + marge Seheli, sans aucun frais
 * (mourabaha). Les dates suivent les règles de l'{@link Echeancier} à partir de {@code depart} ;
 * sans situation salariale connue, les échéances ne sont pas datées et aucune alerte n'est levée.
 *
 * <p>Le contrôle d'endettement ne porte que sur cette simulation : un client ne peut avoir qu'un
 * dossier actif, et la soumission de la demande refait le contrôle complet.
 *
 * @param plafondMensualite part maximale du salaire (arrondie à l'ouguiya inférieure) ; nul sans situation connue
 */
public record SimulationDetaillee(int nombreMois, Montant prixPublic, Montant margeSeheli, Montant frais,
        Montant prixTotal, Montant apport, Montant apportMinimum, Montant resteAFinancer,
        List<EcheanceSimulee> echeances, boolean datesConnues, Montant plafondMensualite,
        boolean endettementDepasse, boolean finContratDepassee) {

    /** Échéance simulée ; {@code date} est nulle quand le jour de paie du client n'est pas connu. */
    public record EcheanceSimulee(int rang, LocalDate date, Montant montant) {

        public EcheanceSimulee {
            Objects.requireNonNull(montant, "montant");
        }
    }

    public SimulationDetaillee {
        Objects.requireNonNull(prixPublic, "prixPublic");
        Objects.requireNonNull(margeSeheli, "margeSeheli");
        Objects.requireNonNull(frais, "frais");
        Objects.requireNonNull(prixTotal, "prixTotal");
        Objects.requireNonNull(apport, "apport");
        Objects.requireNonNull(apportMinimum, "apportMinimum");
        Objects.requireNonNull(resteAFinancer, "resteAFinancer");
        echeances = List.copyOf(echeances);
    }

    /**
     * @param apport   apport choisi, au moins l'apport minimum et strictement inférieur au prix de vente
     * @param depart   date à partir de laquelle court l'échéancier (aujourd'hui)
     * @param paie     situation salariale du client, vide s'il n'a pas encore déclaré son emploi
     * @param partMax  part maximale du salaire net consacrée aux mensualités ({@code endettement.max.fraction})
     * @throws IllegalArgumentException si la durée sort des bornes, si l'apport est hors bornes ou
     *         si le reste est trop petit pour être réparti sur cette durée
     */
    public static SimulationDetaillee simuler(Prix prix, int nombreMois, Montant apport, DureeAutorisee duree,
            LocalDate depart, Optional<SituationPaie> paie, Fraction partMax) {
        Objects.requireNonNull(prix, "prix");
        Objects.requireNonNull(apport, "apport");
        Objects.requireNonNull(duree, "duree");
        Objects.requireNonNull(depart, "depart");
        Objects.requireNonNull(paie, "paie");
        Objects.requireNonNull(partMax, "partMax");
        if (!duree.autorise(nombreMois)) {
            throw new IllegalArgumentException("Durée de " + nombreMois + " mois hors bornes ["
                    + duree.minMois() + " ; " + duree.maxMois() + "]");
        }
        if (!prix.accepteApport(apport) || apport.compareTo(prix.prixVente()) == 0) {
            throw new IllegalArgumentException("Apport " + apport + " hors bornes");
        }
        Montant reste = prix.resteAFinancer(apport);

        List<EcheanceSimulee> echeances = new ArrayList<>(nombreMois);
        boolean finDepassee = false;
        boolean endettementDepasse = false;
        Montant plafond = null;
        if (paie.isPresent()) {
            SituationPaie situation = paie.get();
            Echeancier plan = Echeancier.planifier(reste, nombreMois, depart, situation.jourPaie(), Optional.empty());
            plan.echeances().forEach(e -> echeances.add(new EcheanceSimulee(e.rang(), e.date(), e.montant())));
            LocalDate derniere = plan.derniere().date();
            finDepassee = situation.finContrat().map(fin -> !derniere.isBefore(fin)).orElse(false);
            endettementDepasse = !partMax.admet(plan.mensualiteMaximale(), situation.salaireNet());
            plafond = plafond(situation.salaireNet(), partMax);
        } else {
            List<Montant> montants = reste.repartir(nombreMois);
            if (montants.getLast().estZero()) {
                throw new IllegalArgumentException("Reste " + reste + " trop petit pour " + nombreMois + " mensualités");
            }
            for (int i = 0; i < nombreMois; i++) {
                echeances.add(new EcheanceSimulee(i + 1, null, montants.get(i)));
            }
        }
        return new SimulationDetaillee(nombreMois, prix.prixPublic(), prix.prixVente().moins(prix.prixPublic()),
                Montant.ZERO, prix.prixVente(), apport, prix.apportMinimum(), reste, echeances, paie.isPresent(),
                plafond, endettementDepasse, finDepassee);
    }

    /** Montant des premières mensualités. */
    public Montant mensualite() {
        return echeances.getFirst().montant();
    }

    /** Mensualité la plus élevée : la dernière peut absorber l'écart d'arrondi. */
    public Montant mensualiteMaximale() {
        return echeances.stream().map(EcheanceSimulee::montant).max(Montant::compareTo).orElseThrow();
    }

    public Montant derniereMensualite() {
        return echeances.getLast().montant();
    }

    public boolean derniereDiffere() {
        return derniereMensualite().compareTo(mensualite()) != 0;
    }

    /** Vrai si l'apport choisi dépasse l'apport minimum. */
    public boolean apportSupplementaire() {
        return apport.estSuperieurA(apportMinimum);
    }

    private static Montant plafond(Montant salaireNet, Fraction partMax) {
        BigDecimal valeur = salaireNet.valeur().multiply(BigDecimal.valueOf(partMax.numerateur()))
                .divide(BigDecimal.valueOf(partMax.denominateur()), 0, RoundingMode.DOWN);
        return Montant.de(valeur);
    }
}
