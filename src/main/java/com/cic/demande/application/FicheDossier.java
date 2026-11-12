package com.cic.demande.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cic.demande.domain.Demande;
import com.cic.identite.domain.Client;
import com.cic.identite.domain.Document;
import com.cic.identite.domain.Emploi;

/**
 * Fiche d'un dossier pour l'analyste : la demande et son score, le client, son emploi actuel et
 * ses justificatifs, lus par l'API publique du module identite.
 *
 * @param client        vide si le client est introuvable
 * @param emploi        emploi actuel du client (celui déclaré aujourd'hui) ; vide s'il n'en a plus
 * @param justificatifs justificatifs du client, du plus récent au plus ancien
 */
public record FicheDossier(Demande demande, Optional<Client> client, Optional<Emploi> emploi,
        List<Document> justificatifs) {

    public FicheDossier {
        Objects.requireNonNull(demande, "demande");
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(emploi, "emploi");
        justificatifs = List.copyOf(justificatifs);
    }
}
