package com.cic.demande.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Bon de commande adressé au fournisseur pour le produit d'un dossier, émis quand le paiement
 * fournisseur est validé. Le montant est le prix d'achat fixé à la soumission (prix public moins la
 * remise du fournisseur) : c'est ce que CIC paie au fournisseur. Le contrat, le fournisseur et le
 * produit (autres modules) sont référencés par identifiant.
 *
 * <p>Immuable : une commande ne se modifie pas.
 *
 * @param numero    numéro lisible, par exemple {@code BC-2026-000001}
 * @param validePar identifiant de connexion de la personne qui a validé le paiement fournisseur
 */
public record CommandeFournisseur(UUID id, String numero, UUID demandeId, UUID contratId, String numeroContrat,
        UUID fournisseurId, UUID produitId, String designationProduit, Montant montant, String validePar,
        Instant valideLe) {

    public static final int VALIDE_PAR_LONGUEUR_MAX = Decision.LONGUEUR_ANALYSTE_MAX;

    public CommandeFournisseur {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(numero, "numero");
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(contratId, "contratId");
        Objects.requireNonNull(numeroContrat, "numeroContrat");
        Objects.requireNonNull(fournisseurId, "fournisseurId");
        Objects.requireNonNull(produitId, "produitId");
        Objects.requireNonNull(designationProduit, "designationProduit");
        Objects.requireNonNull(montant, "montant");
        Objects.requireNonNull(validePar, "validePar");
        Objects.requireNonNull(valideLe, "valideLe");
        if (montant.estZero()) {
            throw new IllegalArgumentException("Montant d'une commande nul");
        }
        if (validePar.isBlank() || validePar.length() > VALIDE_PAR_LONGUEUR_MAX) {
            throw new IllegalArgumentException("Identifiant du validateur vide ou trop long");
        }
    }

    /** Commande du produit d'un dossier au prix d'achat fixé à la soumission. */
    public static CommandeFournisseur emettre(String numero, Demande demande, UUID contratId, String numeroContrat,
            String validePar, Instant le) {
        TermesDemande t = demande.termes();
        return new CommandeFournisseur(UUID.randomUUID(), numero, demande.id(), contratId, numeroContrat,
                t.fournisseurId(), t.produitId(), t.designationProduit(), t.prixAchat(), validePar, le);
    }
}
