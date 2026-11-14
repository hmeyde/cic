package com.seheli.contrat.domain;

import java.util.List;
import java.util.Objects;

import com.seheli.commun.domain.Montant;

/**
 * Simulation d'un achat à crédit pour une durée donnée, avec l'apport minimum.
 *
 * <p>Pas d'intérêt (mourabaha) : le prix total payé par le client est le prix de vente, quelle
 * que soit la durée. Le reste à financer est réparti en mensualités égales arrondies à l'ouguiya,
 * la dernière absorbant l'écart.
 *
 * @param nombreMois         nombre de mensualités
 * @param prixTotal          prix total payé par le client (apport + mensualités = prix de vente)
 * @param apport             apport à payer à la commande
 * @param resteAFinancer     prix total − apport
 * @param mensualite         montant des premières mensualités
 * @param derniereMensualite montant de la dernière mensualité (peut différer de quelques ouguiyas)
 */
public record Simulation(int nombreMois, Montant prixTotal, Montant apport, Montant resteAFinancer,
        Montant mensualite, Montant derniereMensualite) {

    public Simulation {
        Objects.requireNonNull(prixTotal, "prixTotal");
        Objects.requireNonNull(apport, "apport");
        Objects.requireNonNull(resteAFinancer, "resteAFinancer");
        Objects.requireNonNull(mensualite, "mensualite");
        Objects.requireNonNull(derniereMensualite, "derniereMensualite");
        if (nombreMois <= 0) {
            throw new IllegalArgumentException("Nombre de mensualités invalide : " + nombreMois);
        }
    }

    /**
     * @throws IllegalArgumentException si la durée sort des bornes autorisées ou si le reste est
     *         trop petit pour être réparti sur cette durée
     */
    public static Simulation simuler(Prix prix, int nombreMois, DureeAutorisee duree) {
        Objects.requireNonNull(prix, "prix");
        Objects.requireNonNull(duree, "duree");
        if (!duree.autorise(nombreMois)) {
            throw new IllegalArgumentException("Durée de " + nombreMois + " mois hors bornes ["
                    + duree.minMois() + " ; " + duree.maxMois() + "]");
        }
        Montant reste = prix.resteAFinancer();
        if (reste.estZero()) {
            throw new IllegalArgumentException("Rien à financer : aucune mensualité à simuler");
        }
        List<Montant> mensualites = reste.repartir(nombreMois);
        if (mensualites.getLast().estZero()) {
            throw new IllegalArgumentException("Reste " + reste + " trop petit pour " + nombreMois + " mensualités");
        }
        return new Simulation(nombreMois, prix.prixVente(), prix.apportMinimum(), reste,
                mensualites.getFirst(), mensualites.getLast());
    }

    /** Vrai si la dernière mensualité diffère des autres (écart d'arrondi). */
    public boolean derniereDiffere() {
        return derniereMensualite.compareTo(mensualite) != 0;
    }
}
