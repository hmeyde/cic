package com.cic.identite.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.identite.domain.Emploi;
import com.cic.identite.domain.EmploiRepository;

public class EmploisEnMemoire implements EmploiRepository {

    private final Map<UUID, Emploi> parClient = new HashMap<>();

    @Override
    public void enregistrer(Emploi emploi) {
        parClient.put(emploi.clientId(), emploi);
    }

    @Override
    public Optional<Emploi> parClient(UUID clientId) {
        return Optional.ofNullable(parClient.get(clientId));
    }
}
