package com.cic.pilotage.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.catalogue.application.FournisseurService;
import com.cic.catalogue.application.FournisseursEnMemoire;
import com.cic.catalogue.domain.Fournisseur;
import com.cic.commun.domain.Montant;
import com.cic.comptabilite.application.ApportCapitalService;
import com.cic.comptabilite.application.ApportsCapitalEnMemoire;
import com.cic.comptabilite.application.ComptabiliteService;
import com.cic.comptabilite.application.EcrituresEnMemoire;
import com.cic.comptabilite.application.IndicateursFinanciersService;
import com.cic.comptabilite.application.SaisieApportCapital;
import com.cic.contrat.application.AutorisationsEnMemoire;
import com.cic.contrat.application.ContratAEtablir;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.application.ContratsEnMemoire;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.EcheanceAPrelever;
import com.cic.contrat.domain.Prix;
import com.cic.identite.application.ClientsEnMemoire;
import com.cic.identite.application.EmploiClientService;
import com.cic.identite.application.EmploisEnMemoire;
import com.cic.identite.domain.Emploi;
import com.cic.identite.domain.ModeRemboursement;
import com.cic.identite.domain.TypeContrat;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;
import com.cic.pilotage.domain.IndicateursRisque;
import com.cic.pilotage.domain.TypeAlerte;

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
}
