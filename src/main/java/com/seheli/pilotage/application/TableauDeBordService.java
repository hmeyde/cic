package com.seheli.pilotage.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
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
import com.seheli.commun.domain.Montant;
import com.seheli.pilotage.domain.IndicateursRisque;
import com.seheli.pilotage.domain.PointEncours;
import com.seheli.pilotage.domain.PointRecouvrement;
import com.seheli.pilotage.domain.PositionDossier;
import com.seheli.pilotage.domain.SeuilsRisque;

/**
 * Assemble le tableau de bord des associés : indicateurs financiers de la comptabilité et indicateurs
 * de risque (voir {@link IndicateursRisque}).
 *
 * <p>Les échéances échues sont celles dont la date est passée (aujourd'hui exclu : le prélèvement du
 * jour peut encore arriver). Le capital disponible est le capital apporté plus le bénéfice réalisé.
 *
 * <p>Les courbes couvrent les {@value #MOIS_AFFICHES} derniers mois, mois en cours compris : encours le
 * dernier jour de chaque mois (aujourd'hui pour le mois en cours) et recouvrement des échéances échues
 * de chaque mois. Ce nombre de mois est un choix d'affichage, pas une règle de gestion.
 */
@Service
public class TableauDeBordService {

    static final int MOIS_AFFICHES = 6;

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
        return new TableauDeBord(i.capitalApporte(), i.capitalEngage(), i.encours(), i.beneficeRealise(), risque,
                courbeEncours(aujourdhui), courbeRecouvrement(aujourdhui));
    }

    private List<PointEncours> courbeEncours(LocalDate aujourdhui) {
        List<PointEncours> points = new ArrayList<>();
        for (YearMonth mois : moisAffiches(aujourdhui)) {
            LocalDate fin = mois.equals(YearMonth.from(aujourdhui)) ? aujourdhui : mois.atEndOfMonth();
            points.add(new PointEncours(mois, indicateurs.encoursAu(fin)));
        }
        return points;
    }

    private List<PointRecouvrement> courbeRecouvrement(LocalDate aujourdhui) {
        LocalDate hier = aujourdhui.minusDays(1);
        List<PointRecouvrement> points = new ArrayList<>();
        for (YearMonth mois : moisAffiches(aujourdhui)) {
            LocalDate debut = mois.atDay(1);
            LocalDate fin = mois.atEndOfMonth().isAfter(hier) ? hier : mois.atEndOfMonth();
            Montant encaisse = debut.isAfter(fin) ? Montant.ZERO : total(contrats.echeancesPayeesEntre(debut, fin));
            Montant impaye = debut.isAfter(fin) ? Montant.ZERO : total(contrats.echeancesEnRetardEntre(debut, fin));
            points.add(new PointRecouvrement(mois, encaisse.plus(impaye), encaisse));
        }
        return points;
    }

    private static List<YearMonth> moisAffiches(LocalDate aujourdhui) {
        YearMonth courant = YearMonth.from(aujourdhui);
        List<YearMonth> mois = new ArrayList<>();
        for (int i = MOIS_AFFICHES - 1; i >= 0; i--) {
            mois.add(courant.minusMonths(i));
        }
        return mois;
    }

    private static Montant total(List<EcheanceAPrelever> echeances) {
        return echeances.stream().map(e -> e.echeance().montant()).reduce(Montant.ZERO, Montant::plus);
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
