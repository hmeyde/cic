package com.cic.contrat.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Contrat de vente à crédit (mourabaha) établi à l'acceptation du dossier : il porte l'offre faite
 * au client. Le prix, l'apport et les échéances sont fixés à cet instant et ne changent plus (pas
 * d'intérêt, pas de pénalité). Le dossier, le client, le produit et le fournisseur sont référencés
 * par identifiant (autres modules).
 *
 * <p>Invariants : apport minimum ≤ apport &lt; prix de vente ; échéances de rang 1 à n, dans
 * l'ordre des dates ; apport + somme des échéances = prix de vente.
 */
public final class Contrat {

    public static final int DESIGNATION_LONGUEUR_MAX = 200;

    private final UUID id;
    private final String numero;
    private final UUID demandeId;
    private final UUID clientId;
    private final UUID produitId;
    private final UUID fournisseurId;
    private final String designationProduit;
    private final Prix prix;
    private final Montant apport;
    private final List<Echeance> echeances;
    private final Instant etabliLe;

    private Contrat(UUID id, String numero, UUID demandeId, UUID clientId, UUID produitId, UUID fournisseurId,
            String designationProduit, Prix prix, Montant apport, List<Echeance> echeances, Instant etabliLe) {
        this.id = Objects.requireNonNull(id, "id");
        this.numero = Objects.requireNonNull(numero, "numero");
        this.demandeId = Objects.requireNonNull(demandeId, "demandeId");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.produitId = Objects.requireNonNull(produitId, "produitId");
        this.fournisseurId = Objects.requireNonNull(fournisseurId, "fournisseurId");
        this.designationProduit = Objects.requireNonNull(designationProduit, "designationProduit");
        this.prix = Objects.requireNonNull(prix, "prix");
        this.apport = Objects.requireNonNull(apport, "apport");
        this.etabliLe = Objects.requireNonNull(etabliLe, "etabliLe");
        Objects.requireNonNull(echeances, "echeances");
        if (numero.isBlank()) {
            throw new IllegalArgumentException("Numéro de contrat vide");
        }
        if (designationProduit.isBlank() || designationProduit.length() > DESIGNATION_LONGUEUR_MAX) {
            throw new IllegalArgumentException("Désignation du produit vide ou trop longue");
        }
        if (!prix.accepteApport(apport) || apport.compareTo(prix.prixVente()) == 0) {
            throw new IllegalArgumentException("Apport " + apport + " hors bornes [" + prix.apportMinimum() + " ; "
                    + prix.prixVente() + "[");
        }
        List<Echeance> triees = echeances.stream().sorted(Comparator.comparingInt(Echeance::rang)).toList();
        if (triees.isEmpty()) {
            throw new IllegalArgumentException("Un contrat a au moins une échéance");
        }
        for (int i = 0; i < triees.size(); i++) {
            if (triees.get(i).rang() != i + 1) {
                throw new IllegalArgumentException("Rangs d'échéance non consécutifs");
            }
            if (i > 0 && !triees.get(i).date().isAfter(triees.get(i - 1).date())) {
                throw new IllegalArgumentException("Échéances non ordonnées par date");
            }
        }
        Montant total = apport.plus(triees.stream().map(Echeance::montant).reduce(Montant.ZERO, Montant::plus));
        if (total.compareTo(prix.prixVente()) != 0) {
            throw new IllegalArgumentException("Apport et échéances (" + total + ") différents du prix de vente ("
                    + prix.prixVente() + ")");
        }
        this.echeances = triees;
    }

    /** Contrat établi à partir de l'échéancier fixé à l'acceptation : toutes les échéances sont A_VENIR. */
    public static Contrat etablir(String numero, UUID demandeId, UUID clientId, UUID produitId, UUID fournisseurId,
            String designationProduit, Prix prix, Montant apport, Echeancier echeancier, Instant le) {
        Objects.requireNonNull(echeancier, "echeancier");
        return new Contrat(UUID.randomUUID(), numero, demandeId, clientId, produitId, fournisseurId,
                designationProduit, prix, apport, echeancier.echeances().stream().map(Echeance::aVenir).toList(), le);
    }

    public static Contrat reconstituer(UUID id, String numero, UUID demandeId, UUID clientId, UUID produitId,
            UUID fournisseurId, String designationProduit, Prix prix, Montant apport, List<Echeance> echeances,
            Instant etabliLe) {
        return new Contrat(id, numero, demandeId, clientId, produitId, fournisseurId, designationProduit, prix,
                apport, echeances, etabliLe);
    }

    /** Reste à rembourser par mensualités : prix de vente − apport. */
    public Montant resteAFinancer() {
        return prix.resteAFinancer(apport);
    }

    /** Capital que CIC engage : prix d'achat − apport (nul si l'apport couvre le prix d'achat). */
    public Montant capitalEngage() {
        return prix.capitalEngage(apport);
    }

    /** Bénéfice brut de CIC : prix de vente − prix d'achat. */
    public Montant beneficeBrut() {
        return prix.beneficeBrut();
    }

    public int nombreMois() {
        return echeances.size();
    }

    public UUID id() {
        return id;
    }

    public String numero() {
        return numero;
    }

    public UUID demandeId() {
        return demandeId;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID produitId() {
        return produitId;
    }

    public UUID fournisseurId() {
        return fournisseurId;
    }

    public String designationProduit() {
        return designationProduit;
    }

    public Prix prix() {
        return prix;
    }

    public Montant apport() {
        return apport;
    }

    /** Échéances par rang croissant. */
    public List<Echeance> echeances() {
        return echeances;
    }

    /** Échéance de ce rang, si le contrat en a une. */
    public Optional<Echeance> echeance(int rang) {
        return echeances.stream().filter(e -> e.rang() == rang).findFirst();
    }

    public Instant etabliLe() {
        return etabliLe;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Contrat c && id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Pas de donnée personnelle : ce texte peut finir dans les journaux. */
    @Override
    public String toString() {
        return "Contrat[" + numero + ", dossier " + demandeId + ", " + echeances.size() + " échéances]";
    }
}
