package com.cic.contrat.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.Prix;

/**
 * Ce qu'il faut pour établir le contrat d'un dossier accepté : conditions figées à la soumission
 * (prix, apport, durée) et calendrier de paie du client.
 *
 * @param jourPaie   jour du mois où le salaire est versé
 * @param finContrat fin du contrat de travail (CDD) ; vide pour un CDI ou un fonctionnaire
 * @param accepteLe  instant de l'acceptation par l'analyste
 */
public record ContratAEtablir(UUID demandeId, UUID clientId, UUID produitId, UUID fournisseurId,
        String designationProduit, Prix prix, Montant apport, int nombreMois, int jourPaie,
        Optional<LocalDate> finContrat, Instant accepteLe) {

    public ContratAEtablir {
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(produitId, "produitId");
        Objects.requireNonNull(fournisseurId, "fournisseurId");
        Objects.requireNonNull(designationProduit, "designationProduit");
        Objects.requireNonNull(prix, "prix");
        Objects.requireNonNull(apport, "apport");
        Objects.requireNonNull(finContrat, "finContrat");
        Objects.requireNonNull(accepteLe, "accepteLe");
    }
}
