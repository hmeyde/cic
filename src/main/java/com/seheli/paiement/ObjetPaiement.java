package com.seheli.paiement;

/**
 * Ce que règle un paiement rapproché d'un dossier : son apport ou l'une de ses échéances. Le type de
 * référence portée par le paiement le désigne ({@code AP…} pour l'apport, {@code EC…} pour une
 * échéance).
 */
public enum ObjetPaiement {
    APPORT,
    ECHEANCE
}
