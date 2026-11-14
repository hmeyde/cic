package com.seheli.contrat.application;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.ConditionsPrix;
import com.seheli.contrat.domain.Prix;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametres;

/**
 * Calcule le prix d'un produit avec les paramètres en vigueur : apport minimum lu dans la
 * table {@code parametre}, remise propre au fournisseur (bornée par les paramètres) et marge
 * propre à la catégorie (par défaut {@code marge.cic.defaut.pct}).
 */
@Service
public class CalculPrixService {

    private final ParametreService parametres;

    public CalculPrixService(ParametreService parametres) {
        this.parametres = parametres;
    }

    /** Calcul avec la marge Seheli par défaut. */
    public Prix calculer(Montant prixPublic, BigDecimal remiseFournisseurPct) {
        Parametres courants = parametres.courants();
        return calculer(courants, prixPublic, remiseFournisseurPct,
                courants.pourcentage(CleParametre.MARGE_CIC_DEFAUT_PCT));
    }

    /**
     * @throws IllegalArgumentException si la remise sort des bornes
     *         {@code remise.fournisseur.min.pct} – {@code remise.fournisseur.max.pct}
     */
    public Prix calculer(Montant prixPublic, BigDecimal remiseFournisseurPct, BigDecimal margeCicPct) {
        return calculer(parametres.courants(), prixPublic, remiseFournisseurPct, margeCicPct);
    }

    private static Prix calculer(Parametres p, Montant prixPublic, BigDecimal remisePct, BigDecimal margePct) {
        Objects.requireNonNull(remisePct, "remiseFournisseurPct");
        BigDecimal min = p.pourcentage(CleParametre.REMISE_FOURNISSEUR_MIN_PCT);
        BigDecimal max = p.pourcentage(CleParametre.REMISE_FOURNISSEUR_MAX_PCT);
        if (remisePct.compareTo(min) < 0 || remisePct.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    "Remise fournisseur " + remisePct + " % hors bornes [" + min + " ; " + max + "] %");
        }
        ConditionsPrix conditions = new ConditionsPrix(remisePct, margePct,
                p.pourcentage(CleParametre.APPORT_MINIMUM_PCT));
        return Prix.calculer(prixPublic, conditions);
    }
}
