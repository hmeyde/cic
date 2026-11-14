package com.seheli.demande.application;

import com.seheli.commun.domain.ErreurMetier;

/** Le produit demandé n'est pas (ou plus) proposé au client. */
public class ProduitNonProposeException extends ErreurMetier {

    public static final String CODE = "demande.produit.indisponible";

    public ProduitNonProposeException() {
        super(CODE, "Produit non proposé");
    }
}
