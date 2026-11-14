package com.seheli.pilotage.domain;

/** Seuils de risque de CLAUDE.md dont le dépassement déclenche une alerte au tableau de bord. */
public enum TypeAlerte {
    /** Au moins un dossier a une échéance impayée depuis le premier palier de provision (30 jours). */
    RETARD_30_JOURS,
    /** Au moins un dossier a une échéance impayée depuis le seuil de passage en perte (180 jours). */
    PERTE,
    /** La part des échéances échues payées est sous le minimum. */
    PRELEVEMENTS_REUSSIS,
    /** L'encours dépasse le % autorisé du capital disponible : aucun nouveau dossier. */
    UTILISATION_CAPITAL,
    /** Un même employeur porte plus que la part autorisée de l'encours. */
    CONCENTRATION_EMPLOYEUR,
    /** Un même fournisseur porte plus que la part autorisée de l'encours. */
    CONCENTRATION_FOURNISSEUR
}
