package com.seheli.pilotage.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.catalogue.application.FournisseurService;
import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.comptabilite.application.IndicateursFinanciersService;
import com.seheli.comptabilite.domain.EncoursDossier;
import com.seheli.comptabilite.domain.IndicateursFinanciers;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.domain.Emploi;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametres;
import com.seheli.pilotage.domain.IndicateursRisque;
import com.seheli.pilotage.domain.PositionDossier;
import com.seheli.pilotage.domain.SeuilsRisque;

/**
 * Assemble le tableau de bord des associés : indicateurs financiers de la comptabilité et indicateurs
 * de risque (voir {@link IndicateursRisque}).
 *
 * <p>Les échéances échues sont celles dont la date est passée (aujourd'hui exclu : le prélèvement du
 * jour peut encore arriver). Le capital disponible est le capital apporté plus le bénéfice réalisé.
 */
@Service
public class TableauDeBordService {

    private final IndicateursFinanciersService indicateurs;
    private final ContratService contrats;
    private final EmploiClientService emplois;
    private final FournisseurService fournisseurs;
    private final ParametreService parametres;
    private final Clock clock;

    public TableauDeBordService(IndicateursFinanciersService indicateurs, ContratService contrats,
            EmploiClientService emplois, FournisseurService fournisseurs, ParametreService parametres, Clock clock) {
        this.indicateurs = indicateurs;
        this.contrats = contrats;
        this.emplois = emplois;
        this.fournisseurs = fournisseurs;
        this.parametres = parametres;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TableauDeBord tableau() {
        LocalDate aujourdhui = LocalDate.now(clock);
        LocalDate hier = aujourdhui.minusDays(1);
        IndicateursFinanciers i = indicateurs.indicateurs();

        List<EcheanceAPrelever> enRetard = contrats.echeancesEnRetardEntre(LocalDate.EPOCH, hier);
        int reussis = contrats.echeancesPayeesEntre(LocalDate.EPOCH, hier).size();
        Map<UUID, LocalDate> plusAncienRetard = enRetard.stream().collect(Collectors.toMap(
                EcheanceAPrelever::demandeId, e -> e.echeance().date(), (a, b) -> a.isBefore(b) ? a : b));

        Map<UUID, String> nomsFournisseurs = fournisseurs.lister().stream()
                .collect(Collectors.toMap(Fournisseur::id, Fournisseur::nom));
        Map<UUID, Optional<String>> employeurs = new HashMap<>();
        List<PositionDossier> positions = new ArrayList<>();
        for (EncoursDossier e : indicateurs.encoursParDossier()) {
            Optional<Contrat> contrat = contrats.parDemande(e.demandeId());
            if (contrat.isEmpty()) {
                continue;
            }
            Contrat c = contrat.get();
            Optional<String> employeur = employeurs.computeIfAbsent(c.clientId(),
                    id -> emplois.parClient(id).map(Emploi::employeur));
            String fournisseur = nomsFournisseurs.getOrDefault(c.fournisseurId(), c.fournisseurId().toString());
            positions.add(new PositionDossier(e.demandeId(), e.restantDu(), employeur, fournisseur,
                    Optional.ofNullable(plusAncienRetard.get(e.demandeId()))));
        }

        IndicateursRisque risque = IndicateursRisque.calculer(positions, reussis, enRetard.size(),
                i.capitalApporte().plus(i.beneficeRealise()), seuils(parametres.courants()), aujourdhui);
        return new TableauDeBord(i.capitalApporte(), i.capitalEngage(), i.encours(), i.beneficeRealise(), risque);
    }

    private static SeuilsRisque seuils(Parametres p) {
        return new SeuilsRisque(p.entier(CleParametre.PROVISION_PALIER1_JOURS),
                p.pourcentage(CleParametre.PROVISION_PALIER1_PCT), p.entier(CleParametre.PROVISION_PALIER2_JOURS),
                p.pourcentage(CleParametre.PROVISION_PALIER2_PCT), p.entier(CleParametre.PERTE_RETARD_JOURS),
                p.pourcentage(CleParametre.RISQUE_EMPLOYEUR_MAX_PCT),
                p.pourcentage(CleParametre.RISQUE_FOURNISSEUR_MAX_PCT),
                p.pourcentage(CleParametre.ENCOURS_CAPITAL_MAX_PCT),
                p.pourcentage(CleParametre.ALERTE_PRELEVEMENTS_REUSSIS_MIN_PCT));
    }
}
