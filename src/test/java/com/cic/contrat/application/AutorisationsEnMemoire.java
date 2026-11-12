package com.cic.contrat.application;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.contrat.domain.AutorisationPrelevement;
import com.cic.contrat.domain.AutorisationPrelevementRepository;
import com.cic.contrat.domain.StatutAutorisation;

public class AutorisationsEnMemoire implements AutorisationPrelevementRepository {

    private final Map<UUID, AutorisationPrelevement> parContrat = new LinkedHashMap<>();
    private int verrous;
    private int misesAJour;

    @Override
    public void enregistrer(AutorisationPrelevement autorisation) {
        if (parContrat.putIfAbsent(autorisation.contratId(), autorisation) != null) {
            throw new IllegalStateException("autorisation_prelevement_contrat_uk");
        }
    }

    @Override
    public void mettreAJour(AutorisationPrelevement autorisation) {
        if (!parContrat.containsKey(autorisation.contratId())) {
            throw new IllegalStateException("Autorisation introuvable");
        }
        parContrat.put(autorisation.contratId(), autorisation);
        misesAJour++;
    }

    @Override
    public Optional<AutorisationPrelevement> parId(UUID id) {
        return parContrat.values().stream().filter(a -> a.id().equals(id)).findFirst();
    }

    @Override
    public Optional<AutorisationPrelevement> parIdVerrouillee(UUID id) {
        verrous++;
        return parId(id);
    }

    @Override
    public Optional<AutorisationPrelevement> parContrat(UUID contratId) {
        return Optional.ofNullable(parContrat.get(contratId));
    }

    @Override
    public List<AutorisationPrelevement> dernieres(Optional<StatutAutorisation> statut, int limite) {
        return parContrat.values().stream()
                .filter(a -> statut.map(s -> a.statut() == s).orElse(true))
                .sorted(Comparator.comparing(AutorisationPrelevement::emiseLe).reversed())
                .limit(limite).toList();
    }

    public List<AutorisationPrelevement> toutes() {
        return List.copyOf(parContrat.values());
    }

    public int verrous() {
        return verrous;
    }

    public int misesAJour() {
        return misesAJour;
    }
}
