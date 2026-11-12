package com.cic.demande.application;

import com.cic.commun.domain.ErreurMetier;

/** Le client a une échéance impayée chez CIC : ses nouveaux achats sont bloqués jusqu'à régularisation. */
public class ClientEnRetardException extends ErreurMetier {

    public static final String CODE = "demande.client.en.retard";

    public ClientEnRetardException() {
        super(CODE, "Le client a une échéance en retard : nouveaux achats bloqués");
    }
}
