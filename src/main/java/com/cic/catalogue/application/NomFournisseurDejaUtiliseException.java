package com.cic.catalogue.application;

import com.cic.commun.domain.ErreurMetier;

/** Un autre fournisseur porte déjà ce nom (comparaison sans tenir compte de la casse). */
public class NomFournisseurDejaUtiliseException extends ErreurMetier {

    public static final String CODE = "bo.fournisseur.nom.deja.utilise";

    public NomFournisseurDejaUtiliseException() {
        super(CODE, "Nom de fournisseur déjà utilisé");
    }
}
