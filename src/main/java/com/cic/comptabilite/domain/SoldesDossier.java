package com.cic.comptabilite.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Soldes comptables d'un dossier, contrepassations comprises (elles annulent l'écriture d'origine).
 *
 * @param demandeId   dossier
 * @param fluxBanque  débits − crédits du compte banque (512) : encaissements du dossier moins le prix
 *                    d'achat payé au fournisseur ; négatif tant que l'argent sorti n'est pas revenu
 * @param achats      débits − crédits du compte achats (607) : prix d'achat payé au fournisseur
 */
public record SoldesDossier(UUID demandeId, BigDecimal fluxBanque, BigDecimal achats) {

    public SoldesDossier {
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(fluxBanque, "fluxBanque");
        Objects.requireNonNull(achats, "achats");
    }

    /** Le fournisseur a été payé (et ce paiement n'a pas été contrepassé). */
    public boolean fournisseurPaye() {
        return achats.signum() > 0;
    }

    /** Argent reçu du client sur ce dossier : apport et échéances encaissés. */
    public BigDecimal encaissements() {
        return fluxBanque.add(achats);
    }

    /** Soldes calculés à partir des écritures (les écritures sans dossier sont ignorées). */
    public static List<SoldesDossier> depuis(List<EcritureComptable> ecritures) {
        Map<UUID, BigDecimal[]> soldes = new LinkedHashMap<>();
        for (EcritureComptable e : ecritures) {
            if (e.demandeId().isEmpty()) {
                continue;
            }
            BigDecimal[] s = soldes.computeIfAbsent(e.demandeId().get(),
                    id -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            for (LigneEcriture l : e.lignes()) {
                BigDecimal signe = l.sens() == Sens.DEBIT ? l.montant().valeur() : l.montant().valeur().negate();
                if (l.compte() == Compte.BANQUE) {
                    s[0] = s[0].add(signe);
                } else if (l.compte() == Compte.ACHATS) {
                    s[1] = s[1].add(signe);
                }
            }
        }
        return soldes.entrySet().stream()
                .map(en -> new SoldesDossier(en.getKey(), en.getValue()[0], en.getValue()[1])).toList();
    }
}
