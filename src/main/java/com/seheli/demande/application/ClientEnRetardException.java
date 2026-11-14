package com.seheli.demande.application;

import com.seheli.commun.domain.ErreurMetier;

/** Le client a une échéance impayée chez Seheli : ses nouveaux achats sont bloqués jusqu'à régularisation. */
public class ClientEnRetardException extends ErreurMetier {

    public static final String CODE = "demande.client.en.retard";

    public ClientEnRetardException() {
        super(CODE, "Le client a une échéance en retard : nouveaux achats bloqués");
    }
}
