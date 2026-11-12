package com.cic.pilotage.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.commun.domain.Montant;
import com.cic.comptabilite.application.ApportCapitalService;
import com.cic.comptabilite.application.ApportsCapitalEnMemoire;
import com.cic.comptabilite.application.EcrituresEnMemoire;
import com.cic.comptabilite.application.IndicateursFinanciersService;
import com.cic.comptabilite.application.SaisieApportCapital;
import com.cic.contrat.application.AutorisationsEnMemoire;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.application.ContratsEnMemoire;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;
import com.cic.pilotage.application.TableauDeBord;
import com.cic.pilotage.application.TableauDeBordService;

/** L'associé voit le capital apporté, le capital engagé, l'encours et le bénéfice réalisé. */
class TableauDeBordControllerTest {

    @Test
    void le_tableau_de_bord_montre_les_quatre_chiffres() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Africa/Nouakchott"));
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        ContratService contrats = new ContratService(new ContratsEnMemoire(), new AutorisationsEnMemoire(),
                parametres, clock);
        EcrituresEnMemoire ecritures = new EcrituresEnMemoire();
        ApportsCapitalEnMemoire apports = new ApportsCapitalEnMemoire();
        new ApportCapitalService(apports, ecritures, new JournalAuditService(new JournalEnMemoire()), clock)
                .saisir(new SaisieApportCapital("Sidi", "500000", "2026-11-01"), "admin");
        TableauDeBordController controller = new TableauDeBordController(
                new TableauDeBordService(new IndicateursFinanciersService(apports, ecritures, contrats)));
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.tableau(model)).isEqualTo("bo/pilotage/tableau");
        assertThat(model.get("tableau")).isEqualTo(
                new TableauDeBord(Montant.de(500_000), Montant.ZERO, Montant.ZERO, Montant.ZERO));
    }
}
