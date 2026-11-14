package com.seheli.comptabilite.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.seheli.commun.domain.Montant;

/**
 * Les quatre chiffres du tableau de bord des associés, tirés de la comptabilité.
 *
 * <p>Seuls comptent les dossiers dont le fournisseur est payé : avant, Seheli n'a rien avancé. Pour
 * chacun, on compare l'argent reçu du client (apport et échéances encaissés) au prix d'achat payé :
 * <ul>
 *   <li>tant que le reçu est inférieur au prix d'achat, la différence est du <b>capital engagé</b> ;</li>
 *   <li>au-delà, l'excédent est du <b>bénéfice réalisé</b> (marge encaissée, en trésorerie) ;</li>
 *   <li>l'<b>encours</b> est le reste dû par le client : prix de vente moins le reçu.</li>
 * </ul>
 * Exemple chiffré : prix d'achat 18 400, prix de vente 20 800, apport 8 320. Fournisseur payé :
 * capital engagé 10 080, encours 12 480, bénéfice 0. Dossier soldé : capital engagé 0, encours 0,
 * bénéfice réalisé 2 400.
 *
 * @param capitalApporte somme des apports en capital des associés
 */
public record IndicateursFinanciers(Montant capitalApporte, Montant capitalEngage, Montant encours,
        Montant beneficeRealise) {

    public IndicateursFinanciers {
        Objects.requireNonNull(capitalApporte, "capitalApporte");
        Objects.requireNonNull(capitalEngage, "capitalEngage");
        Objects.requireNonNull(encours, "encours");
        Objects.requireNonNull(beneficeRealise, "beneficeRealise");
    }

    /**
     * @param prixVente prix de vente fixé au contrat du dossier ; vide si le dossier n'a pas de contrat
     *                  (son encours est alors nul)
     */
    public static IndicateursFinanciers calculer(Montant capitalApporte, List<SoldesDossier> dossiers,
            Function<UUID, Optional<Montant>> prixVente) {
        BigDecimal engage = BigDecimal.ZERO;
        BigDecimal encours = BigDecimal.ZERO;
        BigDecimal benefice = BigDecimal.ZERO;
        for (SoldesDossier d : dossiers) {
            if (!d.fournisseurPaye()) {
                continue;
            }
            BigDecimal flux = d.fluxBanque();
            if (flux.signum() < 0) {
                engage = engage.add(flux.negate());
            } else {
                benefice = benefice.add(flux);
            }
            BigDecimal reste = EncoursDossier.restantDu(d, prixVente);
            if (reste.signum() > 0) {
                encours = encours.add(reste);
            }
        }
        return new IndicateursFinanciers(capitalApporte, Montant.de(engage), Montant.de(encours),
                Montant.de(benefice));
    }
}
