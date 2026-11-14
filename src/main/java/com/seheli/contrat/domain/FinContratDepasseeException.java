package com.seheli.contrat.domain;

import java.time.LocalDate;

import com.seheli.commun.domain.ErreurMetier;

/** Pour un CDD, la dernière échéance ne tombe pas strictement avant la fin du contrat de travail. */
public class FinContratDepasseeException extends ErreurMetier {

    public static final String CODE = "echeancier.fin.contrat.depassee";

    private final LocalDate derniereEcheance;
    private final LocalDate finContrat;

    public FinContratDepasseeException(LocalDate derniereEcheance, LocalDate finContrat) {
        super(CODE, "La dernière échéance (" + derniereEcheance
                + ") doit tomber avant la fin du contrat de travail (" + finContrat + ")");
        this.derniereEcheance = derniereEcheance;
        this.finContrat = finContrat;
    }

    public LocalDate derniereEcheance() {
        return derniereEcheance;
    }

    public LocalDate finContrat() {
        return finContrat;
    }
}
