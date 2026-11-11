package com.cic.demande.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import com.cic.commun.domain.Montant;

/**
 * Faits du dossier nécessaires aux conditions éliminatoires. Rassemblés par l'appelant
 * à partir des modules identité (client, emploi, justificatifs), contrat (mensualité de
 * l'échéancier proposé) et recouvrement (retard en cours).
 *
 * @param nniVerifie              NNI vérifié par le back-office
 * @param pieceIdentiteVerifiee   pièce d'identité vérifiée par le back-office
 * @param dateNaissance           date de naissance du client
 * @param debutContrat            date de début du contrat de travail
 * @param finContrat              date de fin du contrat (CDD) ; vide pour un CDI ou un fonctionnaire
 * @param salaireVerseSurCompte   le salaire est versé sur un compte bancaire (pas en espèces)
 * @param salaireNet              salaire net mensuel
 * @param mensualitesCicEnCours   total des mensualités CIC déjà dues (zéro sinon)
 * @param nouvelleMensualite      mensualité la plus élevée de l'échéancier demandé
 * @param retardEnCours           le client a une échéance CIC en retard
 */
public record DossierEligibilite(
        boolean nniVerifie,
        boolean pieceIdentiteVerifiee,
        LocalDate dateNaissance,
        LocalDate debutContrat,
        Optional<LocalDate> finContrat,
        boolean salaireVerseSurCompte,
        Montant salaireNet,
        Montant mensualitesCicEnCours,
        Montant nouvelleMensualite,
        boolean retardEnCours) {

    public DossierEligibilite {
        Objects.requireNonNull(dateNaissance, "dateNaissance");
        Objects.requireNonNull(debutContrat, "debutContrat");
        Objects.requireNonNull(finContrat, "finContrat");
        Objects.requireNonNull(salaireNet, "salaireNet");
        Objects.requireNonNull(mensualitesCicEnCours, "mensualitesCicEnCours");
        Objects.requireNonNull(nouvelleMensualite, "nouvelleMensualite");
    }
}
