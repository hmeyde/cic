package com.seheli.comptabilite.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.seheli.commun.domain.Montant;

/**
 * Reste dû par le client sur un dossier dont le fournisseur est payé : prix de vente moins l'argent
 * déjà reçu (apport et échéances). C'est la part de ce dossier dans l'encours.
 */
public record EncoursDossier(UUID demandeId, Montant restantDu) {

    public EncoursDossier {
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(restantDu, "restantDu");
    }

    /**
     * Dossiers dont le fournisseur est payé et qui ont encore un reste dû strictement positif.
     *
     * @param prixVente prix de vente fixé au contrat du dossier ; vide si le dossier n'a pas de contrat
     */
    public static List<EncoursDossier> depuis(List<SoldesDossier> dossiers,
            Function<UUID, Optional<Montant>> prixVente) {
        List<EncoursDossier> encours = new ArrayList<>();
        for (SoldesDossier d : dossiers) {
            if (!d.fournisseurPaye()) {
                continue;
            }
            BigDecimal reste = restantDu(d, prixVente);
            if (reste.signum() > 0) {
                encours.add(new EncoursDossier(d.demandeId(), Montant.de(reste)));
            }
        }
        return encours;
    }

    static BigDecimal restantDu(SoldesDossier d, Function<UUID, Optional<Montant>> prixVente) {
        return prixVente.apply(d.demandeId()).map(Montant::valeur).orElse(BigDecimal.ZERO)
                .subtract(d.encaissements());
    }
}
