package com.cic.pilotage.application;

import org.springframework.stereotype.Service;

import com.cic.comptabilite.application.IndicateursFinanciersService;
import com.cic.comptabilite.domain.IndicateursFinanciers;

/** Assemble le tableau de bord des associés à partir des indicateurs de la comptabilité. */
@Service
public class TableauDeBordService {

    private final IndicateursFinanciersService indicateurs;

    public TableauDeBordService(IndicateursFinanciersService indicateurs) {
        this.indicateurs = indicateurs;
    }

    public TableauDeBord tableau() {
        IndicateursFinanciers i = indicateurs.indicateurs();
        return new TableauDeBord(i.capitalApporte(), i.capitalEngage(), i.encours(), i.beneficeRealise());
    }
}
