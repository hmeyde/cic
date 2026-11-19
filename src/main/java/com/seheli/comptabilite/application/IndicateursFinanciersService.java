package com.seheli.comptabilite.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.commun.domain.Montant;
import com.seheli.comptabilite.domain.ApportCapitalRepository;
import com.seheli.comptabilite.domain.EcritureComptableRepository;
import com.seheli.comptabilite.domain.EncoursDossier;
import com.seheli.comptabilite.domain.IndicateursFinanciers;
import com.seheli.contrat.application.ContratService;

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
        return IndicateursFinanciers.calculer(apports.total(), ecritures.soldesParDossier(), this::prixVente);
    }

    /**
     * Encours détaillé par dossier (fournisseur payé, reste dû positif) : sert aux indicateurs de
     * risque (retards, concentration). La somme des restants dus est l'encours des indicateurs.
     */
    @Transactional(readOnly = true)
    public List<EncoursDossier> encoursParDossier() {
        return EncoursDossier.depuis(ecritures.soldesParDossier(), this::prixVente);
    }

    /**
     * Encours tel qu'il était le soir de {@code date} : mêmes règles que {@link #indicateurs()}, en ne
     * comptant que les écritures datées au plus tard de ce jour (courbe d'encours du tableau de bord).
     */
    @Transactional(readOnly = true)
    public Montant encoursAu(LocalDate date) {
        return EncoursDossier.depuis(ecritures.soldesParDossierAu(date), this::prixVente).stream()
                .map(EncoursDossier::restantDu).reduce(Montant.ZERO, Montant::plus);
    }

    private Optional<Montant> prixVente(UUID demandeId) {
        return contrats.parDemande(demandeId).map(c -> c.prix().prixVente());
    }
}
