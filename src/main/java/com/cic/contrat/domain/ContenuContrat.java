package com.cic.contrat.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Ce que le contrat imprimé contient : le contrat (prix fixé, apport, échéances), l'acheteur, la date
 * d'établissement et la date limite pour payer l'apport et faire accepter l'autorisation de
 * prélèvement.
 */
public record ContenuContrat(Contrat contrat, Acheteur acheteur, LocalDate etabliLe, LocalDate dateLimiteApport) {

    public ContenuContrat {
        Objects.requireNonNull(contrat, "contrat");
        Objects.requireNonNull(acheteur, "acheteur");
        Objects.requireNonNull(etabliLe, "etabliLe");
        Objects.requireNonNull(dateLimiteApport, "dateLimiteApport");
    }

    @Override
    public String toString() {
        return "ContenuContrat[" + contrat.numero() + "]";
    }
}
