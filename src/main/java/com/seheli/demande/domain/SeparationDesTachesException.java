package com.seheli.demande.domain;

import com.seheli.commun.domain.ErreurMetier;

/** Séparation des tâches : la personne qui a décidé d'un dossier ne valide pas le paiement fournisseur du même dossier. */
public class SeparationDesTachesException extends ErreurMetier {

    public static final String CODE = "dossier.separation.taches";

    public SeparationDesTachesException() {
        super(CODE, "Le paiement fournisseur doit être validé par une autre personne que l'analyste");
    }
}
