package com.cic.comptabilite.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cic.comptabilite.domain.EcritureComptable;
import com.cic.comptabilite.domain.EcritureComptableRepository;
import com.cic.comptabilite.domain.NatureEcriture;

/** Comme la base : (nature, source) unique, insertion seule. */
public class EcrituresEnMemoire implements EcritureComptableRepository {

    private final List<EcritureComptable> ecritures = new ArrayList<>();

    @Override
    public boolean enregistrerSiNouvelle(EcritureComptable ecriture) {
        if (parSource(ecriture.nature(), ecriture.sourceId()).isPresent()) {
            return false;
        }
        ecritures.add(ecriture);
        return true;
    }

    @Override
    public Optional<EcritureComptable> parId(UUID id) {
        return ecritures.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    @Override
    public Optional<EcritureComptable> parSource(NatureEcriture nature, UUID sourceId) {
        return ecritures.stream().filter(e -> e.nature() == nature && e.sourceId().equals(sourceId)).findFirst();
    }

    @Override
    public List<EcritureComptable> parDemande(UUID demandeId) {
        return ecritures.stream().filter(e -> e.demandeId().filter(demandeId::equals).isPresent()).toList();
    }

    @Override
    public List<EcritureComptable> dernieres(int limite) {
        List<EcritureComptable> toutes = new ArrayList<>(ecritures);
        toutes.sort(Comparator.comparing(EcritureComptable::date).thenComparing(EcritureComptable::passeeLe)
                .reversed());
        return toutes.stream().limit(limite).toList();
    }

    public List<EcritureComptable> toutes() {
        return List.copyOf(ecritures);
    }
}
