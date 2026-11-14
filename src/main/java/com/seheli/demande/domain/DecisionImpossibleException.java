package com.seheli.demande.domain;

import com.seheli.commun.domain.ErreurMetier;

/** Une décision de l'analyste n'est possible que sur un dossier EN_ANALYSE. */
public class DecisionImpossibleException extends ErreurMetier {

    public static final String CODE = "dossier.decision.impossible";

    private final StatutDossier statut;

    public DecisionImpossibleException(StatutDossier statut) {
        super(CODE, "Décision impossible au statut " + statut);
        this.statut = statut;
    }

    public StatutDossier statut() {
        return statut;
    }
}
