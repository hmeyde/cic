package com.seheli.pilotage.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.catalogue.application.FournisseurService;
import com.seheli.catalogue.application.FournisseursEnMemoire;
import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.commun.domain.Montant;
import com.seheli.comptabilite.application.ApportCapitalService;
import com.seheli.comptabilite.application.ApportsCapitalEnMemoire;
import com.seheli.comptabilite.application.ComptabiliteService;
import com.seheli.comptabilite.application.EcrituresEnMemoire;
import com.seheli.comptabilite.application.IndicateursFinanciersService;
import com.seheli.comptabilite.application.SaisieApportCapital;
import com.seheli.contrat.application.AutorisationsEnMemoire;
import com.seheli.contrat.application.ContratAEtablir;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.ContratsEnMemoire;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.contrat.domain.Prix;
import com.seheli.identite.application.ClientsEnMemoire;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.application.EmploisEnMemoire;
import com.seheli.identite.domain.Emploi;
import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeContrat;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.pilotage.domain.IndicateursRisque;
import com.seheli.pilotage.domain.PointEncours;
import com.seheli.pilotage.domain.PointRecouvrement;
import com.seheli.pilotage.domain.TypeAlerte;

/**
 * Le dossier de l'exemple chiffré, accepté le 1er juillet : la première échéance est payée, les trois
 * suivantes sont restées impayées. Le tableau de bord lève les alertes correspondantes.
 */
class TableauDeBordServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant MAINTENANT = Instant.parse("2026-11-12T10:00:00Z");
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 12);

    private final Clock clock = Clock.fixed(MAINTENANT, NOUAKCHOTT);
    private final EcrituresEnMemoire ecritures = new EcrituresEnMemoire();
    private final ApportsCapitalEnMemoire apports = new ApportsCapitalEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(new JournalEnMemoire());
    private final FournisseursEnMemoire fournisseurs = new FournisseursEnMemoire();
    private final EmploisEnMemoire emplois = new EmploisEnMemoire();
    private final ContratService contrats;
    private final ComptabiliteService comptabilite;
    private final ApportCapitalService capital;
    private final TableauDeBordService service;

    TableauDeBordServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        contrats = new ContratService(new ContratsEnMemoire(), new AutorisationsEnMemoire(), parametres, clock);
        comptabilite = new ComptabiliteService(ecritures, contrats, journal, clock);
        capital = new ApportCapitalService(apports, ecritures, journal, clock);
        service = new TableauDeBordService(new IndicateursFinanciersService(apports, ecritures, contrats), contrats,
                new EmploiClientService(new ClientsEnMemoire(), emplois, clock),
                new FournisseurService(fournisseurs, parametres), parametres, clock);
    }

    /** Prix public 20 000, achat 18 400, vente 20 800, apport 8 320, 6 × 2 080 ; fournisseur payé. */
    private Contrat dossierCommande(Instant accepteLe) {
        Fournisseur f = Fournisseur.nouveau("Mauritel", new BigDecimal("8"));
        fournisseurs.enregistrer(f);
        UUID client = UUID.randomUUID();
        emplois.enregistrer(Emploi.nouveau(client, "SNIM", TypeContrat.CDI, LocalDate.of(2020, 1, 1), null,
                Montant.de(60_000), 25, true, false, ModeRemboursement.RETENUE_EMPLOYEUR));
        Contrat c = contrats.etablir(new ContratAEtablir(UUID.randomUUID(), client, UUID.randomUUID(), f.id(),
                "Galaxy A35", new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), 6, 25, Optional.empty(), accepteLe));
        comptabilite.apportEncaisse(UUID.randomUUID(), c.demandeId(), c.id(), c.numero(), c.apport(), AUJOURDHUI);
        comptabilite.paiementFournisseur(UUID.randomUUID(), "BC-2026-000001", c.demandeId(), c.id(),
                c.prix().prixAchat(), accepteLe);
        return c;
    }

    @Test
    void sans_dossier_le_tableau_ne_leve_aucune_alerte() {
        capital.saisir(new SaisieApportCapital("Sidi", "500000", "2026-11-01"), "admin");

        IndicateursRisque r = service.tableau().risque();

        assertThat(r.alertes()).isEmpty();
        assertThat(r.capitalDisponible()).isEqualTo(Montant.de(500_000));
        assertThat(r.utilisationCapitalPct()).contains(new BigDecimal("0.0"));
    }

    @Test
    void des_echeances_impayees_depuis_plus_de_30_jours_levent_les_alertes_de_risque() {
        capital.saisir(new SaisieApportCapital("Sidi", "10000", "2026-06-01"), "admin");
        Contrat c = dossierCommande(Instant.parse("2026-07-01T10:00:00Z"));
        var premiere = c.echeances().getFirst();
        comptabilite.echeanceEncaissee(UUID.randomUUID(), c.demandeId(), c.id(), c.numero(), premiere.rang(),
                premiere.montant(), premiere.date());
        contrats.constaterEcheancePayee(c.id(), premiere.rang());
        for (EcheanceAPrelever e : contrats.echeancesAVenirAvant(AUJOURDHUI)) {
            contrats.constaterEcheanceEnRetard(e.contratId(), e.echeance().rang());
        }
        LocalDate plusAncienRetard = c.echeances().get(1).date();

        TableauDeBord t = service.tableau();
        IndicateursRisque r = t.risque();

        assertThat(t.encours()).isEqualTo(Montant.de(20_800 - 8_320 - 2_080));
        assertThat(plusAncienRetard).isBefore(AUJOURDHUI.minusDays(30)).isAfter(AUJOURDHUI.minusDays(90));
        assertThat(r.dossiersRetard30()).isEqualTo(1);
        assertThat(r.restantDuRetard30()).isEqualTo(Montant.de(10_400));
        assertThat(r.provisions()).isEqualTo(Montant.de(5_200));
        assertThat(r.prelevementsReussis()).isEqualTo(1);
        assertThat(r.prelevementsEchoues()).isEqualTo(3);
        assertThat(r.tauxPrelevementsReussisPct()).contains(new BigDecimal("25.0"));
        assertThat(r.capitalDisponible()).isEqualTo(Montant.de(10_000));
        assertThat(r.utilisationCapitalPct()).contains(new BigDecimal("104.0"));
        assertThat(r.employeurPrincipal().orElseThrow().libelle()).contains("SNIM");
        assertThat(r.fournisseurPrincipal().orElseThrow().libelle()).contains("Mauritel");
        assertThat(r.alertes()).containsExactly(TypeAlerte.RETARD_30_JOURS, TypeAlerte.PRELEVEMENTS_REUSSIS,
                TypeAlerte.UTILISATION_CAPITAL, TypeAlerte.CONCENTRATION_EMPLOYEUR,
                TypeAlerte.CONCENTRATION_FOURNISSEUR);
    }

    @Test
    void les_courbes_couvrent_les_six_derniers_mois_et_finissent_sur_les_chiffres_du_jour() {
        capital.saisir(new SaisieApportCapital("Sidi", "10000", "2026-06-01"), "admin");
        Contrat c = dossierCommande(Instant.parse("2026-07-01T10:00:00Z"));
        var premiere = c.echeances().getFirst();
        comptabilite.echeanceEncaissee(UUID.randomUUID(), c.demandeId(), c.id(), c.numero(), premiere.rang(),
                premiere.montant(), premiere.date());
        contrats.constaterEcheancePayee(c.id(), premiere.rang());
        for (EcheanceAPrelever e : contrats.echeancesAVenirAvant(AUJOURDHUI)) {
            contrats.constaterEcheanceEnRetard(e.contratId(), e.echeance().rang());
        }

        TableauDeBord t = service.tableau();

        assertThat(t.courbeEncours()).extracting(PointEncours::mois).containsExactly(YearMonth.of(2026, 6),
                YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10),
                YearMonth.of(2026, 11));
        assertThat(t.courbeEncours().getFirst().encours()).isEqualTo(Montant.ZERO);
        assertThat(t.courbeEncours().getLast().encours()).isEqualTo(t.encours());
        YearMonth moisPremiere = YearMonth.from(premiere.date());
        PointRecouvrement paye = t.courbeRecouvrement().stream().filter(p -> p.mois().equals(moisPremiere))
                .findFirst().orElseThrow();
        assertThat(paye.tauxPct()).contains(new BigDecimal("100.0"));
        assertThat(t.courbeRecouvrement().stream().map(PointRecouvrement::attendu).reduce(Montant.ZERO, Montant::plus))
                .isEqualTo(Montant.de(4 * 2_080));
        assertThat(t.courbeRecouvrement().stream().map(PointRecouvrement::encaisse).reduce(Montant.ZERO, Montant::plus))
                .isEqualTo(Montant.de(2_080));
        assertThat(t.courbeRecouvrement().getFirst().tauxPct()).isEmpty();
    }
}
