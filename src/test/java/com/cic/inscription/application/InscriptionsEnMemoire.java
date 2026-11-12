package com.cic.inscription.application;

import java.util.ArrayList;
import java.util.List;

import com.cic.inscription.domain.Inscription;
import com.cic.inscription.domain.InscriptionRepository;
import com.cic.inscription.domain.TelephoneDejaInscritException;

public class InscriptionsEnMemoire implements InscriptionRepository {

    private final List<Inscription> inscriptions = new ArrayList<>();

    @Override
    public void enregistrer(Inscription inscription) {
        if (existeTelephone(inscription.telephone())) {
            throw new TelephoneDejaInscritException();
        }
        inscriptions.add(inscription);
    }

    @Override
    public boolean existeTelephone(String telephone) {
        return inscriptions.stream().anyMatch(i -> i.telephone().equals(telephone));
    }

    public List<Inscription> toutes() {
        return List.copyOf(inscriptions);
    }
}
