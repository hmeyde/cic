package com.cic.parametrage.domain;

import static com.cic.parametrage.domain.TypeParametre.ENTIER;
import static com.cic.parametrage.domain.TypeParametre.FRACTION;
import static com.cic.parametrage.domain.TypeParametre.POURCENTAGE;
import static com.cic.parametrage.domain.TypeParametre.TEXTE;

/**
 * Paramètres métier attendus dans la table {@code parametre}. Chacun doit y figurer avec le
 * type déclaré ici, sinon le démarrage échoue.
 */
public enum CleParametre {

    APPORT_MINIMUM_PCT("apport.minimum.pct", POURCENTAGE),
    MARGE_CIC_DEFAUT_PCT("marge.cic.defaut.pct", POURCENTAGE),
    REMISE_FOURNISSEUR_MIN_PCT("remise.fournisseur.min.pct", POURCENTAGE),
    REMISE_FOURNISSEUR_MAX_PCT("remise.fournisseur.max.pct", POURCENTAGE),

    DUREE_MIN_MOIS("duree.min.mois", ENTIER),
    DUREE_MAX_MOIS("duree.max.mois", ENTIER),
    ENDETTEMENT_MAX_FRACTION("endettement.max.fraction", FRACTION),
    DOSSIER_ACTIF_MAX("dossier.actif.max", ENTIER),
    AGE_MINIMUM_ANS("age.minimum.ans", ENTIER),

    DELAI_APPORT_AUTORISATION_JOURS("delai.apport.autorisation.jours", ENTIER),

    RELANCE_AVANT_PRELEVEMENT_JOURS("relance.avant.prelevement.jours", ENTIER),
    RELANCE_ECHEC_PREMIERE_JOURS("relance.echec.premiere.jours", ENTIER),
    RELANCE_ECHEC_SECONDE_JOURS("relance.echec.seconde.jours", ENTIER),

    RISQUE_EMPLOYEUR_MAX_PCT("risque.employeur.max.pct", POURCENTAGE),
    RISQUE_FOURNISSEUR_MAX_PCT("risque.fournisseur.max.pct", POURCENTAGE),
    ENCOURS_CAPITAL_MAX_PCT("encours.capital.max.pct", POURCENTAGE),
    PROVISION_PALIER1_JOURS("provision.palier1.jours", ENTIER),
    PROVISION_PALIER1_PCT("provision.palier1.pct", POURCENTAGE),
    PROVISION_PALIER2_JOURS("provision.palier2.jours", ENTIER),
    PROVISION_PALIER2_PCT("provision.palier2.pct", POURCENTAGE),
    PERTE_RETARD_JOURS("perte.retard.jours", ENTIER),

    SCORE_GRILLE_VERSION("score.grille.version", TEXTE),
    SCORE_SEUIL_FAVORABLE("score.seuil.favorable", ENTIER),
    SCORE_SEUIL_A_EXAMINER("score.seuil.a.examiner", ENTIER),

    CONNEXION_CODE_VALIDITE_MINUTES("connexion.code.validite.minutes", ENTIER),
    CONNEXION_CODE_ESSAIS_MAX("connexion.code.essais.max", ENTIER),
    CONNEXION_CODE_RENVOI_SECONDES("connexion.code.renvoi.secondes", ENTIER),

    CIC_COMPTE_TITULAIRE("cic.compte.titulaire", TEXTE),
    CIC_COMPTE_BANQUE("cic.compte.banque", TEXTE),
    CIC_COMPTE_NUMERO("cic.compte.numero", TEXTE),

    MASRVI_CODE_MARCHAND("masrvi.code.marchand", TEXTE);

    private final String cle;
    private final TypeParametre type;

    CleParametre(String cle, TypeParametre type) {
        this.cle = cle;
        this.type = type;
    }

    /** Clé en base, par exemple {@code apport.minimum.pct}. */
    public String cle() {
        return cle;
    }

    public TypeParametre type() {
        return type;
    }
}
