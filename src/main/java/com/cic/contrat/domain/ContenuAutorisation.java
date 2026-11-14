package com.cic.contrat.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Ce que l'autorisation de prélèvement imprimée contient : l'autorisation, le contrat (référence,
 * échéances), le titulaire du compte à débiter, le compte du vendeur, la banque du client si elle est
 * connue (salaire domicilié à la BMCI), la date d'émission, la date limite pour la faire accepter et
 * la raison sociale du vendeur (paramètre {@code societe.raison-sociale}).
 */
public record ContenuAutorisation(AutorisationPrelevement autorisation, Contrat contrat, Acheteur titulaire,
        CompteBeneficiaire beneficiaire, Optional<String> banqueTitulaire, LocalDate emiseLe,
        LocalDate dateLimite, String raisonSociale) {

    public ContenuAutorisation {
        Objects.requireNonNull(autorisation, "autorisation");
        Objects.requireNonNull(contrat, "contrat");
        Objects.requireNonNull(titulaire, "titulaire");
        Objects.requireNonNull(beneficiaire, "beneficiaire");
        Objects.requireNonNull(banqueTitulaire, "banqueTitulaire");
        Objects.requireNonNull(emiseLe, "emiseLe");
        Objects.requireNonNull(dateLimite, "dateLimite");
        Objects.requireNonNull(raisonSociale, "raisonSociale");
        if (!autorisation.contratId().equals(contrat.id())) {
            throw new IllegalArgumentException("L'autorisation n'est pas celle du " + contrat);
        }
    }

    @Override
    public String toString() {
        return "ContenuAutorisation[" + contrat.numero() + ", " + autorisation.type() + "]";
    }
}
