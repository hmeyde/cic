package com.cic.contrat.domain;

import com.cic.commun.domain.ErreurMetier;

/** L'autorisation est déjà transmise à la banque, acceptée, active ou révoquée : son document signé ne change plus. */
public class DepotDocumentSigneImpossibleException extends ErreurMetier {

    public static final String CODE = "autorisation.document.depot-impossible";

    public DepotDocumentSigneImpossibleException(StatutAutorisation statut) {
        super(CODE, "Dépôt du document signé impossible au statut " + statut);
    }
}
