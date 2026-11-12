package com.cic.comptabilite.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.comptabilite.domain.ApportCapitalRepository;
import com.cic.comptabilite.domain.EcritureComptableRepository;
import com.cic.comptabilite.domain.IndicateursFinanciers;
import com.cic.contrat.application.ContratService;

/**
 * Chiffres du tableau de bord des associés : capital apporté, capital engagé, encours et bénéfice
 * réalisé, calculés à partir des apports en capital et des écritures comptables (voir
 * {@link IndicateursFinanciers}). Le prix de vente de chaque dossier vient de son contrat.
 */
@Service
public class IndicateursFinanciersService {

    private final ApportCapitalRepository apports;
    private final EcritureComptableRepository ecritures;
    private final ContratService contrats;

    public IndicateursFinanciersService(ApportCapitalRepository apports, EcritureComptableRepository ecritures,
            ContratService contrats) {
        this.apports = apports;
        this.ecritures = ecritures;
        this.contrats = contrats;
    }

    @Transactional(readOnly = true)
    public IndicateursFinanciers indicateurs() {
        return IndicateursFinanciers.calculer(apports.total(), ecritures.soldesParDossier(),
                demandeId -> contrats.parDemande(demandeId).map(c -> c.prix().prixVente()));
    }
}
