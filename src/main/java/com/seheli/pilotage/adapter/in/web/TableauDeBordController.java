package com.seheli.pilotage.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.seheli.pilotage.application.TableauDeBord;
import com.seheli.pilotage.application.TableauDeBordService;
import com.seheli.pilotage.domain.PointEncours;
import com.seheli.pilotage.domain.PointRecouvrement;
import com.seheli.pilotage.domain.SeuilsRisque;

/**
 * Back-office : tableau de bord des associés, rôles ASSOCIE et ADMIN (voir {@code SecurityConfig}).
 * Les graphiques sont calculés ici et dessinés en SVG par le gabarit : aucune bibliothèque ni script.
 */
@Controller
class TableauDeBordController {

    static final String CHEMIN = "/bo/pilotage";
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final TableauDeBordService tableaux;

    TableauDeBordController(TableauDeBordService tableaux) {
        this.tableaux = tableaux;
    }

    @GetMapping(CHEMIN)
    String tableau(Model model, Locale locale) {
        boolean rtl = locale != null && "ar".equals(locale.getLanguage());
        TableauDeBord t = tableaux.tableau();
        SeuilsRisque s = t.risque().seuils();
        model.addAttribute("tableau", t);
        model.addAttribute("courbeEncours", Courbe.de(
                t.courbeEncours().stream().map(p -> p.mois().atDay(1)).toList(),
                t.courbeEncours().stream().map(PointEncours::encours).map(m -> m.valeur()).toList(),
                null, null, rtl));
        List<LocalDate> moisRecouvrement = t.courbeRecouvrement().stream().map(p -> p.mois().atDay(1)).toList();
        model.addAttribute("courbeRecouvrement", Courbe.de(moisRecouvrement,
                t.courbeRecouvrement().stream().map(PointRecouvrement::tauxPct).map(o -> o.orElse(null)).toList(),
                s.prelevementsReussisMinPct(), CENT, rtl));
        model.addAttribute("barresEmployeurs", Barres.de(t.risque().repartitionEmployeurs(), s.employeurMaxPct(), rtl));
        model.addAttribute("barresFournisseurs",
                Barres.de(t.risque().repartitionFournisseurs(), s.fournisseurMaxPct(), rtl));
        return "bo/pilotage/tableau";
    }
}
