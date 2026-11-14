package com.cic.demande.domain;

import static com.cic.demande.domain.ConditionEliminatoire.AGE_MINIMUM;
import static com.cic.demande.domain.ConditionEliminatoire.AUCUN_RETARD;
import static com.cic.demande.domain.ConditionEliminatoire.CONTRAT_EN_COURS;
import static com.cic.demande.domain.ConditionEliminatoire.ENDETTEMENT;
import static com.cic.demande.domain.ConditionEliminatoire.IDENTITE_VERIFIEE;
import static com.cic.demande.domain.ConditionEliminatoire.SALAIRE_SUR_COMPTE;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;

import com.cic.commun.domain.Montant;

/**
 * Conditions éliminatoires du score v1 (CLAUDE.md) :
 * <ol>
 *   <li>NNI et pièce d'identité vérifiés ;</li>
 *   <li>âge minimum atteint ({@code age.minimum.ans}, 18) ;</li>
 *   <li>contrat de travail en cours ;</li>
 *   <li>salaire versé sur un compte ;</li>
 *   <li>mensualités CIC ≤ part maximale du salaire net ({@code endettement.max.fraction}, 1/3) ;</li>
 *   <li>aucun retard en cours chez CIC.</li>
 * </ol>
 * Toutes les conditions sont évaluées (pas d'arrêt à la première) pour que l'analyste voie
 * chaque raison. Le résultat recommande ; l'analyste décide.
 */
public final class ConditionsEliminatoires {

    private ConditionsEliminatoires() {
    }

    /**
     * @param date date d'évaluation (issue de la {@code Clock} de l'application)
     */
    public static Eligibilite evaluer(DossierEligibilite dossier, ReglesEligibilite regles, LocalDate date) {
        Objects.requireNonNull(dossier, "dossier");
        Objects.requireNonNull(regles, "regles");
        Objects.requireNonNull(date, "date");
        return new Eligibilite(List.of(
                identite(dossier),
                age(dossier, regles, date),
                contrat(dossier, date),
                salaireSurCompte(dossier),
                endettement(dossier, regles),
                retard(dossier)));
    }

    static ResultatCondition identite(DossierEligibilite d) {
        if (d.nniVerifie() && d.pieceIdentiteVerifiee()) {
            return ResultatCondition.satisfaite(IDENTITE_VERIFIEE, "NNI et pièce d'identité vérifiés");
        }
        if (!d.nniVerifie() && !d.pieceIdentiteVerifiee()) {
            return ResultatCondition.nonSatisfaite(IDENTITE_VERIFIEE, "NNI et pièce d'identité non vérifiés");
        }
        return ResultatCondition.nonSatisfaite(IDENTITE_VERIFIEE,
                d.nniVerifie() ? "Pièce d'identité non vérifiée" : "NNI non vérifié");
    }

    static ResultatCondition age(DossierEligibilite d, ReglesEligibilite r, LocalDate date) {
        if (d.dateNaissance().isAfter(date)) {
            return ResultatCondition.nonSatisfaite(AGE_MINIMUM, "Date de naissance postérieure à la date d'évaluation");
        }
        int age = Period.between(d.dateNaissance(), date).getYears();
        if (age >= r.ageMinimumAns()) {
            return ResultatCondition.satisfaite(AGE_MINIMUM, age + " ans (minimum " + r.ageMinimumAns() + ")");
        }
        return ResultatCondition.nonSatisfaite(AGE_MINIMUM, age + " ans, moins que le minimum de "
                + r.ageMinimumAns() + " ans");
    }

    static ResultatCondition contrat(DossierEligibilite d, LocalDate date) {
        if (d.debutContrat().isAfter(date)) {
            return ResultatCondition.nonSatisfaite(CONTRAT_EN_COURS,
                    "Contrat de travail pas encore commencé (début le " + d.debutContrat() + ")");
        }
        return d.finContrat()
                .filter(fin -> !fin.isAfter(date))
                .map(fin -> ResultatCondition.nonSatisfaite(CONTRAT_EN_COURS,
                        "Contrat de travail terminé le " + fin))
                .orElseGet(() -> ResultatCondition.satisfaite(CONTRAT_EN_COURS,
                        d.finContrat().map(fin -> "CDD en cours jusqu'au " + fin)
                                .orElse("Contrat de travail sans date de fin en cours")));
    }

    static ResultatCondition salaireSurCompte(DossierEligibilite d) {
        return d.salaireVerseSurCompte()
                ? ResultatCondition.satisfaite(SALAIRE_SUR_COMPTE, "Salaire versé sur un compte bancaire")
                : ResultatCondition.nonSatisfaite(SALAIRE_SUR_COMPTE, "Salaire non versé sur un compte bancaire");
    }

    static ResultatCondition endettement(DossierEligibilite d, ReglesEligibilite r) {
        Montant total = d.mensualitesCicEnCours().plus(d.nouvelleMensualite());
        String detail = "mensualités Seheli " + total + " pour un salaire net de " + d.salaireNet()
                + " (plafond " + r.endettementMax() + ")";
        if (!d.salaireNet().estZero() && r.endettementMax().admet(total, d.salaireNet())) {
            return ResultatCondition.satisfaite(ENDETTEMENT, "Endettement respecté : " + detail);
        }
        return ResultatCondition.nonSatisfaite(ENDETTEMENT, "Endettement dépassé : " + detail);
    }

    static ResultatCondition retard(DossierEligibilite d) {
        return d.retardEnCours()
                ? ResultatCondition.nonSatisfaite(AUCUN_RETARD, "Échéance Seheli en retard")
                : ResultatCondition.satisfaite(AUCUN_RETARD, "Aucun retard en cours chez Seheli");
    }
}
