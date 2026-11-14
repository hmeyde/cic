package com.seheli.contrat.domain;

import com.seheli.commun.domain.ErreurMetier;

/** Un dossier n'a qu'un contrat : il est établi une seule fois, à l'acceptation. */
public class ContratDejaEtabliException extends ErreurMetier {

    public static final String CODE = "contrat.deja.etabli";

    public ContratDejaEtabliException() {
        super(CODE, "Un contrat est déjà établi pour ce dossier");
    }
}
