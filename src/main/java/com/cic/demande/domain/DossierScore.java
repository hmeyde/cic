package com.cic.demande.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import com.cic.commun.domain.Montant;

/**
 * Faits du dossier nécessaires à la note sur 100. Rassemblés par l'appelant à partir des
 * modules identité (emploi, garant), contrat (prix, échéancier proposé) et recouvrement
 * (historique des plans).
 *
 * @param typeContrat            type de contrat de travail
 * @param debutContrat           date d'entrée chez l'employeur actuel
 * @param finContrat             date de fin du CDD ; vide pour un CDI ou un fonctionnaire
 * @param derniereEcheance       date de la dernière échéance de l'échéancier proposé
 * @param modeRemboursement      mode de remboursement choisi
 * @param salaireNet             salaire net mensuel
 * @param mensualitesCicEnCours  total des mensualités CIC déjà dues (zéro sinon)
 * @param nouvelleMensualite     mensualité la plus élevée de l'échéancier proposé
 * @param prixVente              prix de vente du produit
 * @param apportMinimum          apport minimum exigé (40 % du prix de vente)
 * @param apport                 apport proposé par le client
 * @param garant                 le client présente un garant
 * @param plansRembourseATemps   nombre de plans CIC déjà soldés sans retard
 */
public record DossierScore(
        TypeContrat typeContrat,
        LocalDate debutContrat,
        Optional<LocalDate> finContrat,
        LocalDate derniereEcheance,
        ModeRemboursement modeRemboursement,
        Montant salaireNet,
        Montant mensualitesCicEnCours,
        Montant nouvelleMensualite,
        Montant prixVente,
        Montant apportMinimum,
        Montant apport,
        boolean garant,
        int plansRembourseATemps) {

    public DossierScore {
        Objects.requireNonNull(typeContrat, "typeContrat");
        Objects.requireNonNull(debutContrat, "debutContrat");
        Objects.requireNonNull(finContrat, "finContrat");
        Objects.requireNonNull(derniereEcheance, "derniereEcheance");
        Objects.requireNonNull(modeRemboursement, "modeRemboursement");
        Objects.requireNonNull(salaireNet, "salaireNet");
        Objects.requireNonNull(mensualitesCicEnCours, "mensualitesCicEnCours");
        Objects.requireNonNull(nouvelleMensualite, "nouvelleMensualite");
        Objects.requireNonNull(prixVente, "prixVente");
        Objects.requireNonNull(apportMinimum, "apportMinimum");
        Objects.requireNonNull(apport, "apport");
        if (typeContrat == TypeContrat.CDD && finContrat.isEmpty()) {
            throw new IllegalArgumentException("Un CDD a une date de fin");
        }
        if (plansRembourseATemps < 0) {
            throw new IllegalArgumentException("Nombre de plans négatif : " + plansRembourseATemps);
        }
    }
}
