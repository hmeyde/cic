package com.seheli.contrat.application;

import java.util.Objects;

import com.seheli.contrat.domain.AutorisationPrelevement;

/**
 * Autorisation dans la liste du suivi au back-office.
 *
 * @param client prénom et nom du client ; vide s'il est introuvable
 */
public record LigneAutorisation(AutorisationPrelevement autorisation, String numeroContrat, String designation,
        String client) {

    public LigneAutorisation {
        Objects.requireNonNull(autorisation, "autorisation");
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        Objects.requireNonNull(designation, "designation");
        Objects.requireNonNull(client, "client");
    }
}
