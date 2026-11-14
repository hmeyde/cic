package com.seheli.inscription.domain;

import com.seheli.commun.domain.ErreurMetier;

/** Pré-inscription refusée : un champ manque ou est mal formé. Le code désigne le champ fautif. */
public class InscriptionInvalideException extends ErreurMetier {

    public static final String NOM_INVALIDE = "inscription.nom.invalide";
    public static final String TELEPHONE_INVALIDE = "inscription.telephone.invalide";
    public static final String EMPLOYEUR_INVALIDE = "inscription.employeur.invalide";
    public static final String TYPE_CONTRAT_INVALIDE = "inscription.type-contrat.invalide";
    public static final String TRANCHE_SALAIRE_INVALIDE = "inscription.tranche-salaire.invalide";
    public static final String PRODUIT_INVALIDE = "inscription.produit.invalide";
    public static final String CONSENTEMENT_MANQUANT = "inscription.consentement.manquant";

    public InscriptionInvalideException(String code) {
        super(code, "Inscription refusée : " + code);
    }

    /** Nom du champ du formulaire concerné, pour placer le message à côté. */
    public String champ() {
        return switch (code()) {
            case NOM_INVALIDE -> "nom";
            case TELEPHONE_INVALIDE -> "telephone";
            case EMPLOYEUR_INVALIDE -> "employeur";
            case TYPE_CONTRAT_INVALIDE -> "typeContrat";
            case TRANCHE_SALAIRE_INVALIDE -> "trancheSalaire";
            case PRODUIT_INVALIDE -> "produitSouhaite";
            default -> "consentement";
        };
    }
}
