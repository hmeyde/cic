package com.seheli.contrat.domain;

import com.seheli.commun.domain.ErreurMetier;
import com.seheli.commun.domain.Fraction;
import com.seheli.commun.domain.Montant;

/** Le total des mensualités Seheli du client dépasserait la part autorisée de son salaire net. */
public class EndettementDepasseException extends ErreurMetier {

    public static final String CODE = "echeancier.endettement.depasse";

    private final Montant totalMensualites;
    private final Montant salaireNet;
    private final Fraction partMax;

    public EndettementDepasseException(Montant totalMensualites, Montant salaireNet, Fraction partMax) {
        super(CODE, "Total des mensualités Seheli (" + totalMensualites + ") supérieur à " + partMax
                + " du salaire net (" + salaireNet + ")");
        this.totalMensualites = totalMensualites;
        this.salaireNet = salaireNet;
        this.partMax = partMax;
    }

    public Montant totalMensualites() {
        return totalMensualites;
    }

    public Montant salaireNet() {
        return salaireNet;
    }

    public Fraction partMax() {
        return partMax;
    }
}
