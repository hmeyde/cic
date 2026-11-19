package com.seheli.pilotage.domain;

/**
 * Seuils de risque de CLAUDE.md dont le dépassement déclenche une alerte au tableau de bord.
 *
 * <p>Une alerte critique appelle une décision immédiate des associés (perte à constater, plus aucun
 * nouveau dossier possible) ; les autres sont à surveiller.
 */
public enum TypeAlerte {
    /** Au moins un dossier a une échéance impayée depuis le premier palier de provision (30 jours). */
    RETARD_30_JOURS(false),
    /** Au moins un dossier a une échéance impayée depuis le seuil de passage en perte (180 jours). */
    PERTE(true),
    /** La part des échéances échues payées est sous le minimum. */
    PRELEVEMENTS_REUSSIS(false),
    /** L'encours dépasse le % autorisé du capital disponible : aucun nouveau dossier. */
    UTILISATION_CAPITAL(true),
    /** Un même employeur porte plus que la part autorisée de l'encours. */
    CONCENTRATION_EMPLOYEUR(false),
    /** Un même fournisseur porte plus que la part autorisée de l'encours. */
    CONCENTRATION_FOURNISSEUR(false);

    private final boolean critique;

    TypeAlerte(boolean critique) {
        this.critique = critique;
    }

    public boolean critique() {
        return critique;
    }
}
