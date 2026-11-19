package com.seheli.demande.adapter.in.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.StatutEcheance;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.paiement.domain.ReferenceEcheance;

/**
 * Écran « Mon remboursement » : combien d'échéances sont payées sur le total, ce qui reste à payer,
 * la prochaine échéance et la liste des échéances avec leur statut. Le graphique (anneau de progression,
 * un arc par échéance) est calculé ici, côté serveur, et rendu en SVG par le gabarit : aucun JavaScript.
 *
 * <p>Prochaine échéance : la première EN_RETARD s'il y en a une (c'est elle qu'il faut régulariser),
 * sinon la première A_VENIR ; aucune une fois tout payé.
 *
 * @param statut                  statut du dossier
 * @param payees                  nombre d'échéances PAYEE
 * @param total                   nombre d'échéances du contrat
 * @param montantPaye             somme des échéances payées
 * @param montantRestant          somme des échéances non payées (A_VENIR et EN_RETARD)
 * @param prochaine               prochaine échéance, {@code null} si tout est payé
 * @param joursAvantProchaine     jours avant la prochaine échéance (négatif : jours de retard)
 * @param enRetard                au moins une échéance EN_RETARD
 * @param totalEnRetard           somme des échéances EN_RETARD
 * @param referenceRegularisation référence Masrvi de la première échéance en retard, sinon {@code null}
 * @param codeMarchand            code marchand Masrvi de Seheli
 * @param arcs                    arcs de l'anneau, un par échéance, dans l'ordre des rangs
 */
public record RemboursementClient(String numero, String designation, StatutDossier statut, int payees, int total,
        Montant montantPaye, Montant montantRestant, Echeance prochaine, Long joursAvantProchaine, boolean enRetard,
        Montant totalEnRetard, String referenceRegularisation, String codeMarchand, List<Echeance> echeances,
        List<Arc> arcs) {

    /** Rayon de l'anneau dans un repère de 120 × 120. */
    public static final int RAYON = 52;
    /** Espace entre deux arcs, en unités du repère. */
    static final double ECART = 3.0;
    static final double CIRCONFERENCE = 2 * Math.PI * RAYON;

    public RemboursementClient {
        echeances = List.copyOf(echeances);
        arcs = List.copyOf(arcs);
    }

    /**
     * Arc de l'anneau pour une échéance : longueur tracée et écart, décalage depuis le haut de l'anneau
     * (valeurs prêtes pour {@code stroke-dasharray} et {@code stroke-dashoffset}).
     */
    public record Arc(int rang, StatutEcheance statut, String dasharray, String dashoffset) {
    }

    /** Le remboursement a commencé : le produit est remis, les échéances courent ou sont réglées. */
    public static boolean concerne(StatutDossier statut) {
        return switch (statut) {
            case LIVREE, EN_REMBOURSEMENT, EN_RETARD, RECOUVREMENT, SOLDEE -> true;
            default -> false;
        };
    }

    public int rayon() {
        return RAYON;
    }

    public boolean solde() {
        return payees == total;
    }

    /** Part payée, de 0 à 100, en nombre d'échéances. */
    public int pourcentage() {
        return total == 0 ? 0 : payees * 100 / total;
    }

    static RemboursementClient depuis(StatutDossier statut, Contrat contrat, String codeMarchand,
            LocalDate aujourdhui) {
        Objects.requireNonNull(statut, "statut");
        Objects.requireNonNull(contrat, "contrat");
        Objects.requireNonNull(aujourdhui, "aujourdhui");
        List<Echeance> echeances = contrat.echeances();
        int payees = (int) echeances.stream().filter(Echeance::estPayee).count();
        Montant paye = somme(echeances.stream().filter(Echeance::estPayee).toList());
        Montant restant = somme(echeances.stream().filter(e -> !e.estPayee()).toList());
        List<Echeance> retards = echeances.stream().filter(Echeance::estEnRetard).toList();
        Echeance prochaine = retards.isEmpty()
                ? echeances.stream().filter(e -> e.statut() == StatutEcheance.A_VENIR).findFirst().orElse(null)
                : retards.getFirst();
        Long jours = prochaine == null ? null : ChronoUnit.DAYS.between(aujourdhui, prochaine.date());
        String reference = retards.isEmpty() ? null
                : ReferenceEcheance.pourEcheance(contrat.numero(), retards.getFirst().rang()).valeur();
        return new RemboursementClient(contrat.numero(), contrat.designationProduit(), statut, payees,
                echeances.size(), paye, restant, prochaine, jours, !retards.isEmpty(), somme(retards), reference,
                codeMarchand, echeances, arcs(echeances));
    }

    /**
     * Un arc par échéance, de même longueur, séparés par un petit écart (pas d'écart pour une seule
     * échéance). Le premier arc part du haut de l'anneau ; le gabarit tourne le cercle de -90°.
     */
    static List<Arc> arcs(List<Echeance> echeances) {
        int n = echeances.size();
        if (n == 0) {
            return List.of();
        }
        double pas = CIRCONFERENCE / n;
        double ecart = n == 1 ? 0 : ECART;
        List<Arc> arcs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Echeance e = echeances.get(i);
            arcs.add(new Arc(e.rang(), e.statut(), nombre(pas - ecart) + " " + nombre(CIRCONFERENCE - pas + ecart),
                    nombre(-i * pas)));
        }
        return arcs;
    }

    private static String nombre(double valeur) {
        return BigDecimal.valueOf(valeur).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static Montant somme(List<Echeance> echeances) {
        return echeances.stream().map(Echeance::montant).reduce(Montant.ZERO, Montant::plus);
    }
}
