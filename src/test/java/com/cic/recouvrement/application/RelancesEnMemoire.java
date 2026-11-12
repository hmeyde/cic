package com.cic.recouvrement.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.cic.recouvrement.domain.Relance;
import com.cic.recouvrement.domain.RelanceRepository;
import com.cic.recouvrement.domain.TypeRelance;

public class RelancesEnMemoire implements RelanceRepository {

    private final List<Relance> relances = new ArrayList<>();

    @Override
    public boolean existe(UUID echeanceId, TypeRelance type) {
        return relances.stream().anyMatch(r -> r.echeanceId().equals(echeanceId) && r.type() == type);
    }

    @Override
    public void enregistrer(Relance relance) {
        if (existe(relance.echeanceId(), relance.type())) {
            throw new IllegalStateException("relance_echeance_type_uk");
        }
        relances.add(relance);
    }

    @Override
    public List<Relance> parContrat(UUID contratId) {
        return relances.stream().filter(r -> r.contratId().equals(contratId))
                .sorted(Comparator.comparing(Relance::envoyeeLe).reversed()).toList();
    }

    public List<Relance> toutes() {
        return List.copyOf(relances);
    }
}
