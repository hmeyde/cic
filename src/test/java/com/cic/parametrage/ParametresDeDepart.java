package com.cic.parametrage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cic.parametrage.domain.CleParametre;
import com.cic.parametrage.domain.Parametre;

/** Valeurs de départ de CLAUDE.md, identiques aux migrations (V2, V7, V15, V18, V24). */
public final class ParametresDeDepart {

    private static final Map<CleParametre, String> VALEURS = Map.ofEntries(
            Map.entry(CleParametre.APPORT_MINIMUM_PCT, "40"),
            Map.entry(CleParametre.MARGE_CIC_DEFAUT_PCT, "4"),
            Map.entry(CleParametre.REMISE_FOURNISSEUR_MIN_PCT, "5"),
            Map.entry(CleParametre.REMISE_FOURNISSEUR_MAX_PCT, "10"),
            Map.entry(CleParametre.DUREE_MIN_MOIS, "3"),
            Map.entry(CleParametre.DUREE_MAX_MOIS, "6"),
            Map.entry(CleParametre.ENDETTEMENT_MAX_FRACTION, "1/3"),
            Map.entry(CleParametre.DOSSIER_ACTIF_MAX, "1"),
            Map.entry(CleParametre.AGE_MINIMUM_ANS, "18"),
            Map.entry(CleParametre.DELAI_APPORT_AUTORISATION_JOURS, "7"),
            Map.entry(CleParametre.RELANCE_AVANT_PRELEVEMENT_JOURS, "1"),
            Map.entry(CleParametre.RELANCE_ECHEC_PREMIERE_JOURS, "3"),
            Map.entry(CleParametre.RELANCE_ECHEC_SECONDE_JOURS, "7"),
            Map.entry(CleParametre.RISQUE_EMPLOYEUR_MAX_PCT, "20"),
            Map.entry(CleParametre.RISQUE_FOURNISSEUR_MAX_PCT, "20"),
            Map.entry(CleParametre.ENCOURS_CAPITAL_MAX_PCT, "90"),
            Map.entry(CleParametre.PROVISION_PALIER1_JOURS, "30"),
            Map.entry(CleParametre.PROVISION_PALIER1_PCT, "50"),
            Map.entry(CleParametre.PROVISION_PALIER2_JOURS, "90"),
            Map.entry(CleParametre.PROVISION_PALIER2_PCT, "100"),
            Map.entry(CleParametre.PERTE_RETARD_JOURS, "180"),
            Map.entry(CleParametre.SCORE_GRILLE_VERSION, "v1"),
            Map.entry(CleParametre.SCORE_SEUIL_FAVORABLE, "70"),
            Map.entry(CleParametre.SCORE_SEUIL_A_EXAMINER, "50"),
            Map.entry(CleParametre.CONNEXION_CODE_VALIDITE_MINUTES, "5"),
            Map.entry(CleParametre.CONNEXION_CODE_ESSAIS_MAX, "5"),
            Map.entry(CleParametre.CONNEXION_CODE_RENVOI_SECONDES, "60"),
            Map.entry(CleParametre.CIC_COMPTE_TITULAIRE, "CIC SARL"),
            Map.entry(CleParametre.CIC_COMPTE_BANQUE, "BMCI"),
            Map.entry(CleParametre.CIC_COMPTE_NUMERO, "00020 00101 00001234567 53"),
            Map.entry(CleParametre.MASRVI_CODE_MARCHAND, "012345"),
            Map.entry(CleParametre.CIC_WHATSAPP_NUMERO, "20000000"));

    private ParametresDeDepart() {
    }

    public static List<Parametre> lignes() {
        List<Parametre> lignes = new ArrayList<>();
        for (CleParametre cle : CleParametre.values()) {
            lignes.add(new Parametre(cle.cle(), VALEURS.get(cle), cle.type()));
        }
        return lignes;
    }

    /** Lignes de départ où la valeur de {@code cle} est remplacée. */
    public static List<Parametre> avec(CleParametre cle, String valeur) {
        return lignes().stream()
                .map(p -> p.cle().equals(cle.cle()) ? new Parametre(p.cle(), valeur, p.type()) : p)
                .toList();
    }
}
