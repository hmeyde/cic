package com.seheli.demande.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.seheli.audit.domain.EntreeJournal;
import com.seheli.contrat.domain.AutorisationPrelevement;
import com.seheli.demande.domain.Demande;
import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.Document;
import com.seheli.identite.domain.Emploi;

/**
 * Fiche d'un dossier pour l'analyste : la demande et son score, le client, son emploi actuel et
 * ses justificatifs, lus par l'API publique du module identite, et le journal d'audit du dossier.
 *
 * @param client        vide si le client est introuvable
 * @param emploi        emploi actuel du client (celui déclaré aujourd'hui) ; vide s'il n'en a plus
 * @param justificatifs justificatifs du client, du plus récent au plus ancien
 * @param journal       journal d'audit du dossier, du plus ancien au plus récent
 * @param autorisation  autorisation de prélèvement, lue par l'API publique du module contrat ; vide
 *                      tant que le dossier n'a pas de contrat
 */
public record FicheDossier(Demande demande, Optional<Client> client, Optional<Emploi> emploi,
        List<Document> justificatifs, List<EntreeJournal> journal, Optional<AutorisationPrelevement> autorisation) {

    public FicheDossier {
        Objects.requireNonNull(demande, "demande");
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(emploi, "emploi");
        justificatifs = List.copyOf(justificatifs);
        journal = List.copyOf(journal);
        Objects.requireNonNull(autorisation, "autorisation");
    }
}
