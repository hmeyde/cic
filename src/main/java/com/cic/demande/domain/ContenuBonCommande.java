package com.cic.demande.domain;

import java.time.LocalDate;
import java.util.Objects;

import com.cic.commun.domain.Montant;

/**
 * Ce qui figure sur le bon de commande : la commande, le fournisseur et le prix public du produit.
 * Aucune donnée personnelle du client : le fournisseur livre la société acheteuse (raison sociale lue
 * dans le paramètre {@code societe.raison-sociale}), qui remet le produit au client.
 */
public record ContenuBonCommande(CommandeFournisseur commande, String nomFournisseur, Montant prixPublic,
        LocalDate le, String raisonSociale) {

    public ContenuBonCommande {
        Objects.requireNonNull(commande, "commande");
        Objects.requireNonNull(nomFournisseur, "nomFournisseur");
        Objects.requireNonNull(prixPublic, "prixPublic");
        Objects.requireNonNull(le, "le");
        Objects.requireNonNull(raisonSociale, "raisonSociale");
    }

    /** Remise du fournisseur, en ouguiyas : prix public moins prix d'achat. */
    public Montant remise() {
        return prixPublic.moins(commande.montant());
    }
}
