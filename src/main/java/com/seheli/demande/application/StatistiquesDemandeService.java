package com.seheli.demande.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.demande.domain.DemandeRepository;

/**
 * Chiffres agrégés sur les demandes, pour les autres modules (aucune donnée personnelle).
 *
 * <p>Une demande « complète » est une demande de crédit soumise par le parcours client de bout en bout
 * (compte vérifié, emploi déclaré, produit choisi, simulation acceptée), quel que soit son statut
 * ensuite : c'est l'étape qui suit la simple pré-inscription.
 */
@Service
public class StatistiquesDemandeService {

    private final DemandeRepository demandes;

    public StatistiquesDemandeService(DemandeRepository demandes) {
        this.demandes = demandes;
    }

    @Transactional(readOnly = true)
    public long nombreDemandesCompletes() {
        return demandes.nombre();
    }
}
