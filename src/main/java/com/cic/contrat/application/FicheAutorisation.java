package com.cic.contrat.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cic.audit.domain.EntreeJournal;
import com.cic.contrat.domain.AutorisationPrelevement;
import com.cic.contrat.domain.Contrat;
import com.cic.identite.domain.Client;

/**
 * Suivi d'une autorisation au back-office : l'autorisation, son contrat, le client et le journal.
 *
 * @param client  vide si le client est introuvable
 * @param journal journal d'audit de l'autorisation, du plus ancien au plus récent
 */
public record FicheAutorisation(AutorisationPrelevement autorisation, Contrat contrat, Optional<Client> client,
        List<EntreeJournal> journal) {

    public FicheAutorisation {
        Objects.requireNonNull(autorisation, "autorisation");
        Objects.requireNonNull(contrat, "contrat");
        Objects.requireNonNull(client, "client");
        journal = List.copyOf(journal);
    }
}
