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
 * Établit l'échéancier d'un contrat avec les paramètres en vigueur : bornes de durée
 * ({@code duree.min.mois} – {@code duree.max.mois}) et part maximale du salaire net
 * consacrée aux mensualités CIC ({@code endettement.max.fraction}).
 */
@Service
public class EcheancierService {

    private final ParametreService parametres;

    public EcheancierService(ParametreService parametres) {
        this.parametres = parametres;
    }

    /**
     * Établit l'échéancier puis applique les contrôles : durée autorisée, fin du CDD et
     * endettement.
     *
     * @param salaireNet            salaire net mensuel du client
     * @param mensualitesCicEnCours mensualités CIC déjà dues par le client (zéro s'il n'en a pas)
     * @throws com.cic.contrat.domain.FinContratDepasseeException si la dernière échéance ne tombe
     *         pas avant la fin du CDD
     * @throws com.cic.contrat.domain.EndettementDepasseException si le total des mensualités CIC
     *         dépasse la part autorisée du salaire net
     */
    public Echeancier etablir(Montant resteAFinancer, int nombreMois, LocalDate dateDepart, int jourPaie,
            Optional<LocalDate> finContrat, Montant salaireNet, Montant mensualitesCicEnCours) {
        Parametres p = parametres.courants();
        Echeancier echeancier = Echeancier.etablir(resteAFinancer, nombreMois, dureeAutorisee(p), dateDepart,
                jourPaie, finContrat);
        echeancier.verifierEndettement(salaireNet, mensualitesCicEnCours,
                p.fraction(CleParametre.ENDETTEMENT_MAX_FRACTION));
        return echeancier;
    }

    public DureeAutorisee dureeAutorisee() {
        return dureeAutorisee(parametres.courants());
    }

    private static DureeAutorisee dureeAutorisee(Parametres p) {
        return new DureeAutorisee(p.entier(CleParametre.DUREE_MIN_MOIS), p.entier(CleParametre.DUREE_MAX_MOIS));
    }
}
