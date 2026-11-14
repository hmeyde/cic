package com.seheli.demande.application;

import java.util.Objects;

import com.seheli.demande.domain.Demande;

/**
 * Dossier de la file des analystes : la demande (avec son score) et le nom du client, lu dans le
 * module identite. Nom vide si le client est introuvable.
 */
public record DossierEnAnalyse(Demande demande, String nomClient) {

    public DossierEnAnalyse {
        Objects.requireNonNull(demande, "demande");
        Objects.requireNonNull(nomClient, "nomClient");
    }
}
