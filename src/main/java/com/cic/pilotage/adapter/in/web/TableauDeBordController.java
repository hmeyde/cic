package com.cic.pilotage.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.cic.pilotage.application.TableauDeBordService;

/** Back-office : tableau de bord des associés, rôles ASSOCIE et ADMIN (voir {@code SecurityConfig}). */
@Controller
class TableauDeBordController {

    static final String CHEMIN = "/bo/pilotage";

    private final TableauDeBordService tableaux;

    TableauDeBordController(TableauDeBordService tableaux) {
        this.tableaux = tableaux;
    }

    @GetMapping(CHEMIN)
    String tableau(Model model) {
        model.addAttribute("tableau", tableaux.tableau());
        return "bo/pilotage/tableau";
    }
}
