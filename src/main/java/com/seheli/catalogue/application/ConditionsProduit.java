package com.seheli.catalogue.application;

import java.util.Objects;
import java.util.UUID;

import com.seheli.contrat.domain.Prix;

/**
 * Conditions d'un produit proposé, pour une demande de crédit : fournisseur et prix complet calculé
 * avec les paramètres en vigueur (dont le prix d'achat, réservé au back-office).
 */
public record ConditionsProduit(UUID produitId, UUID fournisseurId, String designation, Prix prix) {

    public ConditionsProduit {
        Objects.requireNonNull(produitId, "produitId");
        Objects.requireNonNull(fournisseurId, "fournisseurId");
        Objects.requireNonNull(designation, "designation");
        Objects.requireNonNull(prix, "prix");
    }
}
