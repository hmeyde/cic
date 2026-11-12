package com.cic.demande.application;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandeRepository;

public class DemandesEnMemoire implements DemandeRepository {

    private final Map<UUID, Demande> demandes = new LinkedHashMap<>();
    private int verrous;

    @Override
    public void enregistrer(Demande demande) {
        demandes.put(demande.id(), demande);
    }

    @Override
    public Optional<Demande> parId(UUID id) {
        return Optional.ofNullable(demandes.get(id));
    }

    @Override
    public List<Demande> parClient(UUID clientId) {
        return demandes.values().stream().filter(d -> d.clientId().equals(clientId))
                .sorted(Comparator.comparing(Demande::soumiseLe).reversed()).toList();
    }

    @Override
    public void verrouillerClient(UUID clientId) {
        verrous++;
    }

    public int verrous() {
        return verrous;
    }
}
