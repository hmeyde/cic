package com.cic.comptabilite.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.cic.commun.domain.Montant;
import com.cic.comptabilite.domain.ApportCapital;
import com.cic.comptabilite.domain.ApportCapitalRepository;

public class ApportsCapitalEnMemoire implements ApportCapitalRepository {

    private final List<ApportCapital> apports = new ArrayList<>();

    @Override
    public void enregistrer(ApportCapital apport) {
        apports.add(apport);
    }

    @Override
    public List<ApportCapital> tous() {
        return apports.stream().sorted(Comparator.comparing(ApportCapital::verseLe)
                .thenComparing(ApportCapital::saisiLe).reversed()).toList();
    }

    @Override
    public Montant total() {
        return apports.stream().map(ApportCapital::montant).reduce(Montant.ZERO, Montant::plus);
    }
}
