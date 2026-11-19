package com.seheli.contrat.domain;

import java.util.Objects;

import com.seheli.commun.domain.Montant;

/**
 * Prix d'un produit vendu à crédit selon la logique de mourabaha : Seheli achète au fournisseur
 * avec une remise et revend au prix public plus une marge fixée à la signature.
 *
 * <p>Chaque montant est arrondi à l'ouguiya (HALF_UP) une seule fois, à partir du prix public :
 * <ul>
 *   <li>prix d'achat = prix public − remise fournisseur ;</li>
 *   <li>prix de vente = prix public + marge Seheli ;</li>
 *   <li>apport minimum = taux d'apport × prix de vente.</li>
 * </ul>
 * Immuable : le prix ne change jamais après la signature.
 */
public record Prix(Montant prixPublic, Montant prixAchat, Montant prixVente, Montant apportMinimum) {

    public Prix {
        Objects.requireNonNull(prixPublic, "prixPublic");
        Objects.requireNonNull(prixAchat, "prixAchat");
        Objects.requireNonNull(prixVente, "prixVente");
        Objects.requireNonNull(apportMinimum, "apportMinimum");
    }

    /** @throws IllegalArgumentException si le prix public est nul */
    public static Prix calculer(Montant prixPublic, ConditionsPrix conditions) {
        Objects.requireNonNull(prixPublic, "prixPublic");
        Objects.requireNonNull(conditions, "conditions");
        if (prixPublic.estZero()) {
            throw new IllegalArgumentException("Le prix public doit être strictement positif");
        }
        Montant prixAchat = prixPublic.moins(prixPublic.pourcentage(conditions.remiseFournisseurPct()));
        Montant prixVente = prixPublic.plus(prixPublic.pourcentage(conditions.margeSeheliPct()));
        Montant apportMinimum = prixVente.pourcentage(conditions.apportMinimumPct());
        return new Prix(prixPublic, prixAchat, prixVente, apportMinimum);
    }

    /** Reste à rembourser par mensualités avec l'apport minimum. */
    public Montant resteAFinancer() {
        return resteAFinancer(apportMinimum);
    }

    /** @throws IllegalArgumentException si l'apport est insuffisant ou dépasse le prix de vente */
    public Montant resteAFinancer(Montant apport) {
        exigerApportValide(apport);
        return prixVente.moins(apport);
    }

    /** Capital que Seheli engage avec l'apport minimum : prix d'achat − apport. */
    public Montant capitalEngage() {
        return capitalEngage(apportMinimum);
    }

    /** Capital engagé par Seheli ; nul si l'apport couvre déjà le prix d'achat. */
    public Montant capitalEngage(Montant apport) {
        exigerApportValide(apport);
        return apport.estSuperieurOuEgalA(prixAchat) ? Montant.ZERO : prixAchat.moins(apport);
    }

    /** Bénéfice brut de Seheli : prix de vente − prix d'achat. */
    public Montant beneficeBrut() {
        return prixVente.moins(prixAchat);
    }

    /** Vrai si l'apport atteint le minimum sans dépasser le prix de vente. */
    public boolean accepteApport(Montant apport) {
        Objects.requireNonNull(apport, "apport");
        return apport.estSuperieurOuEgalA(apportMinimum) && apport.estInferieurOuEgalA(prixVente);
    }

    private void exigerApportValide(Montant apport) {
        if (!accepteApport(apport)) {
            throw new IllegalArgumentException("Apport " + apport + " hors bornes [" + apportMinimum
                    + " ; " + prixVente + "]");
        }
    }
}
