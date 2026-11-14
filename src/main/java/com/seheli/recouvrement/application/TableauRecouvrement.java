package com.seheli.recouvrement.application;

import java.time.LocalDate;
import java.util.List;

/**
 * Écran du recouvrement : prélèvements à venir (jusqu'au {@code aVenirJusquAu} inclus), réussis
 * (échéances payées dont la date tombe à partir du {@code reussisDepuis}) et en échec (échéances
 * EN_RETARD, toutes), chacun par date.
 */
public record TableauRecouvrement(List<PrelevementSuivi> aVenir, List<PrelevementSuivi> reussis,
        List<PrelevementSuivi> enEchec, LocalDate aVenirJusquAu, LocalDate reussisDepuis) {

    public TableauRecouvrement {
        aVenir = List.copyOf(aVenir);
        reussis = List.copyOf(reussis);
        enEchec = List.copyOf(enEchec);
    }
}
