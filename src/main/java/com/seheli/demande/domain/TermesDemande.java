package com.seheli.demande.domain;

import java.util.Objects;
import java.util.UUID;

import com.seheli.commun.domain.Montant;

/**
 * Conditions demandées par le client, figées à la soumission : produit choisi, prix calculé avec les
 * paramètres du jour, apport proposé et mensualités. Le produit et le fournisseur sont référencés
 * par identifiant (module catalogue) ; la désignation est recopiée pour l'affichage.
 *
 * <p>Invariants : apport minimum ≤ apport &lt; prix de vente ; apport + (n − 1) × mensualité +
 * dernière mensualité = prix de vente (pas d'intérêt, la dernière mensualité absorbe l'arrondi).
 *
 * @param prixAchat          prix d'achat au fournisseur (jamais montré au client)
 * @param mensualite         montant des premières mensualités
 * @param derniereMensualite montant de la dernière mensualité
 */
public record TermesDemande(UUID produitId, UUID fournisseurId, String designationProduit, Montant prixPublic,
        Montant prixAchat, Montant prixVente, Montant apportMinimum, Montant apport, int nombreMois,
        Montant mensualite, Montant derniereMensualite) {

    public static final int DESIGNATION_LONGUEUR_MAX = 200;

    public TermesDemande {
        Objects.requireNonNull(produitId, "produitId");
        Objects.requireNonNull(fournisseurId, "fournisseurId");
        Objects.requireNonNull(designationProduit, "designationProduit");
        Objects.requireNonNull(prixPublic, "prixPublic");
        Objects.requireNonNull(prixAchat, "prixAchat");
        Objects.requireNonNull(prixVente, "prixVente");
        Objects.requireNonNull(apportMinimum, "apportMinimum");
        Objects.requireNonNull(apport, "apport");
        Objects.requireNonNull(mensualite, "mensualite");
        Objects.requireNonNull(derniereMensualite, "derniereMensualite");
        if (designationProduit.isBlank() || designationProduit.length() > DESIGNATION_LONGUEUR_MAX) {
            throw new IllegalArgumentException("Désignation du produit vide ou trop longue");
        }
        if (nombreMois <= 0) {
            throw new IllegalArgumentException("Nombre de mensualités invalide : " + nombreMois);
        }
        if (apport.estInferieurA(apportMinimum) || apport.estSuperieurOuEgalA(prixVente)) {
            throw new IllegalArgumentException("Apport " + apport + " hors bornes [" + apportMinimum + " ; "
                    + prixVente + "[");
        }
        Montant total = apport.plus(mensualite.fois(nombreMois - 1)).plus(derniereMensualite);
        if (total.compareTo(prixVente) != 0) {
            throw new IllegalArgumentException("Apport et mensualités (" + total + ") différents du prix de vente ("
                    + prixVente + ")");
        }
    }

    /** Reste à rembourser par mensualités : prix de vente − apport. */
    public Montant resteAFinancer() {
        return prixVente.moins(apport);
    }

    /** Mensualité la plus élevée, retenue pour l'endettement. */
    public Montant mensualiteMaximale() {
        return derniereMensualite.estSuperieurA(mensualite) ? derniereMensualite : mensualite;
    }
}
