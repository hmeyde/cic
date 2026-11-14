package com.seheli.demande.domain;

/**
 * Où en sont les échéances du contrat d'un dossier, au moment d'une décision du recouvrement :
 * nombre d'échéances EN_RETARD et nombre d'échéances pas encore payées (A_VENIR ou EN_RETARD).
 */
public record SituationEcheances(int enRetard, int nonPayees) {

    public SituationEcheances {
        if (enRetard < 0 || nonPayees < enRetard) {
            throw new IllegalArgumentException("Situation d'échéances incohérente : " + enRetard + ", " + nonPayees);
        }
    }

    /** Aucune échéance n'est EN_RETARD : le retard est rattrapé. */
    public boolean aucunRetard() {
        return enRetard == 0;
    }

    /** Toutes les échéances sont payées : le prix de vente est entièrement réglé. */
    public boolean toutPaye() {
        return nonPayees == 0;
    }
}
