package com.cic.contrat.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.ContratRepository;

public class ContratsEnMemoire implements ContratRepository {

    private final Map<UUID, Contrat> parDemande = new LinkedHashMap<>();
    private long ordre;

    @Override
    public void enregistrer(Contrat contrat) {
        if (parDemande.putIfAbsent(contrat.demandeId(), contrat) != null) {
            throw new IllegalStateException("contrat_demande_uk");
        }
    }

    @Override
    public Optional<Contrat> parId(UUID id) {
        return parDemande.values().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Contrat> parDemande(UUID demandeId) {
        return Optional.ofNullable(parDemande.get(demandeId));
    }

    @Override
    public long prochainNumeroOrdre() {
        return ++ordre;
    }

    public List<Contrat> tous() {
        return List.copyOf(parDemande.values());
    }
}
