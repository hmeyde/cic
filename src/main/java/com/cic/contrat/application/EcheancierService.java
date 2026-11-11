package com.cic.contrat.application;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.DureeAutorisee;
import com.cic.contrat.domain.Echeancier;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;
import com.cic.parametrage.domain.Parametres;

/**
 * Établit l'échéancier d'un contrat avec les bornes de durée en vigueur
 * ({@code duree.min.mois} – {@code duree.max.mois}).
 */
@Service
public class EcheancierService {

    private final ParametreService parametres;

    public EcheancierService(ParametreService parametres) {
        this.parametres = parametres;
    }

    public Echeancier etablir(Montant resteAFinancer, int nombreMois, LocalDate dateDepart, int jourPaie,
            Optional<LocalDate> finContrat) {
        return Echeancier.etablir(resteAFinancer, nombreMois, dureeAutorisee(), dateDepart, jourPaie, finContrat);
    }

    public DureeAutorisee dureeAutorisee() {
        Parametres p = parametres.courants();
        return new DureeAutorisee(p.entier(CleParametre.DUREE_MIN_MOIS), p.entier(CleParametre.DUREE_MAX_MOIS));
    }
}
