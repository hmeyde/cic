package com.cic.parametrage.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cic.commun.domain.Fraction;

/**
 * Jeu complet et cohérent de paramètres, immuable. Sa création vérifie que chaque
 * {@link CleParametre} est présente avec le bon type ; les clés inconnues sont ignorées.
 */
public final class Parametres {

    private final Map<CleParametre, Parametre> valeurs;

    private Parametres(Map<CleParametre, Parametre> valeurs) {
        this.valeurs = valeurs;
    }

    /** @throws ParametreInvalideException en listant toutes les clés manquantes ou mal typées */
    public static Parametres de(Collection<Parametre> lignes) {
        Map<String, Parametre> parCle = new HashMap<>();
        lignes.forEach(p -> parCle.put(p.cle(), p));

        Map<CleParametre, Parametre> valeurs = new EnumMap<>(CleParametre.class);
        List<String> erreurs = new ArrayList<>();
        for (CleParametre cle : CleParametre.values()) {
            Parametre p = parCle.get(cle.cle());
            if (p == null) {
                erreurs.add(cle.cle() + " manquant");
            } else if (p.type() != cle.type()) {
                erreurs.add(cle.cle() + " de type " + p.type() + " au lieu de " + cle.type());
            } else {
                valeurs.put(cle, p);
            }
        }
        if (!erreurs.isEmpty()) {
            throw new ParametreInvalideException("Paramètres invalides : " + String.join(", ", erreurs));
        }
        verifierCoherence(valeurs);
        return new Parametres(valeurs);
    }

    private static void verifierCoherence(Map<CleParametre, Parametre> v) {
        exigerOrdre(v, CleParametre.DUREE_MIN_MOIS, CleParametre.DUREE_MAX_MOIS);
        exigerOrdre(v, CleParametre.REMISE_FOURNISSEUR_MIN_PCT, CleParametre.REMISE_FOURNISSEUR_MAX_PCT);
        exigerOrdre(v, CleParametre.PROVISION_PALIER1_JOURS, CleParametre.PROVISION_PALIER2_JOURS);
        exigerOrdre(v, CleParametre.PROVISION_PALIER2_JOURS, CleParametre.PERTE_RETARD_JOURS);
        exigerOrdre(v, CleParametre.RELANCE_ECHEC_PREMIERE_JOURS, CleParametre.RELANCE_ECHEC_SECONDE_JOURS);
        exigerOrdre(v, CleParametre.SCORE_SEUIL_A_EXAMINER, CleParametre.SCORE_SEUIL_FAVORABLE);
    }

    private static void exigerOrdre(Map<CleParametre, Parametre> v, CleParametre min, CleParametre max) {
        BigDecimal a = new BigDecimal(v.get(min).valeur());
        BigDecimal b = new BigDecimal(v.get(max).valeur());
        if (a.compareTo(b) > 0) {
            throw new ParametreInvalideException(
                    "Paramètres incohérents : " + min.cle() + " (" + a + ") > " + max.cle() + " (" + b + ")");
        }
    }

    public int entier(CleParametre cle) {
        return Integer.parseInt(valeur(cle, TypeParametre.ENTIER));
    }

    /** Pourcentage tel que stocké, par exemple {@code 40} pour 40 %. */
    public BigDecimal pourcentage(CleParametre cle) {
        return new BigDecimal(valeur(cle, TypeParametre.POURCENTAGE));
    }

    public Fraction fraction(CleParametre cle) {
        return Fraction.de(valeur(cle, TypeParametre.FRACTION));
    }

    public String texte(CleParametre cle) {
        return valeur(cle, TypeParametre.TEXTE);
    }

    public int taille() {
        return valeurs.size();
    }

    private String valeur(CleParametre cle, TypeParametre attendu) {
        if (cle.type() != attendu) {
            throw new IllegalArgumentException(cle.cle() + " est de type " + cle.type() + ", pas " + attendu);
        }
        return valeurs.get(cle).valeur();
    }
}
