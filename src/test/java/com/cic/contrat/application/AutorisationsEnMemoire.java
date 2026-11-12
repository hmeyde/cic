package com.cic.contrat.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.contrat.domain.AutorisationPrelevement;
import com.cic.contrat.domain.AutorisationPrelevementRepository;

public class AutorisationsEnMemoire implements AutorisationPrelevementRepository {

    private final Map<UUID, AutorisationPrelevement> parContrat = new LinkedHashMap<>();

    @Override
    public void enregistrer(AutorisationPrelevement autorisation) {
        if (parContrat.putIfAbsent(autorisation.contratId(), autorisation) != null) {
            throw new IllegalStateException("autorisation_prelevement_contrat_uk");
        }
    }

    @Override
    public Optional<AutorisationPrelevement> parContrat(UUID contratId) {
        return Optional.ofNullable(parContrat.get(contratId));
    }

    public List<AutorisationPrelevement> toutes() {
        return List.copyOf(parContrat.values());
    }
}
