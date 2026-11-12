package com.cic.comptabilite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.audit.domain.ObjetAudite;
import com.cic.commun.domain.Montant;
import com.cic.comptabilite.domain.ApportCapital;
import com.cic.comptabilite.domain.ApportCapitalInvalideException;
import com.cic.comptabilite.domain.IndicateursFinanciers;
import com.cic.comptabilite.domain.NatureEcriture;
import com.cic.contrat.application.AutorisationsEnMemoire;
import com.cic.contrat.application.ContratAEtablir;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.application.ContratsEnMemoire;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Echeance;
import com.cic.contrat.domain.Prix;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

/** Les quatre chiffres des associés suivent le dossier de l'exemple chiffré, de la commande au solde. */
class IndicateursFinanciersServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant MAINTENANT = Instant.parse("2026-11-12T10:00:00Z");
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 12);

    private final Clock clock = Clock.fixed(MAINTENANT, NOUAKCHOTT);
    private final EcrituresEnMemoire ecritures = new EcrituresEnMemoire();
    private final ApportsCapitalEnMemoire apports = new ApportsCapitalEnMemoire();
    private final JournalEnMemoire journalEnMemoire = new JournalEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(journalEnMemoire);
    private final ContratService contrats;
    private final ComptabiliteService comptabilite;
    private final ApportCapitalService capital;
    private final IndicateursFinanciersService indicateurs;

    IndicateursFinanciersServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        contrats = new ContratService(new ContratsEnMemoire(), new AutorisationsEnMemoire(), parametres, clock);
        comptabilite = new ComptabiliteService(ecritures, contrats, journal, clock);
        capital = new ApportCapitalService(apports, ecritures, journal, clock);
        indicateurs = new IndicateursFinanciersService(apports, ecritures, contrats);
    }

    /** Prix public 20 000, achat 18 400, vente 20 800, apport 8 320, 6 × 2 080. */
    private Contrat contrat() {
        return contrats.etablir(new ContratAEtablir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), 6, 25, Optional.empty(), MAINTENANT.minusSeconds(86_400)));
    }

    private static IndicateursFinanciers chiffres(long apporte, long engage, long encours, long benefice) {
        return new IndicateursFinanciers(Montant.de(apporte), Montant.de(engage), Montant.de(encours),
                Montant.de(benefice));
    }

    @Test
    void sans_apport_ni_dossier_les_quatre_chiffres_sont_nuls() {
        assertThat(indicateurs.indicateurs()).isEqualTo(chiffres(0, 0, 0, 0));
    }

    @Test
    void les_apports_en_capital_s_additionnent_et_sont_comptabilises_et_journalises() {
        ApportCapital a = capital.saisir(new SaisieApportCapital("Sidi", "300 000", "2026-11-01"), "admin");
        capital.saisir(new SaisieApportCapital("Aminetou", "200000", "2026-11-12"), "admin");

        assertThat(indicateurs.indicateurs().capitalApporte()).isEqualTo(Montant.de(500_000));
        assertThat(capital.tous()).extracting(ApportCapital::associe).containsExactly("Aminetou", "Sidi");
        assertThat(ecritures.parSource(NatureEcriture.APPORT_CAPITAL, a.id())).isPresent();
        assertThat(journal.historique(new ObjetAudite(ApportCapitalService.TYPE_OBJET, a.id()))).hasSize(1);
    }

    @Test
    void une_saisie_d_apport_invalide_est_refusee_sans_rien_enregistrer() {
        assertThatThrownBy(() -> capital.saisir(new SaisieApportCapital("Sidi", "12,5", "2026-11-12"), "admin"))
                .isInstanceOfSatisfying(ApportCapitalInvalideException.class,
                        e -> assertThat(e.code()).isEqualTo(ApportCapitalInvalideException.MONTANT_INVALIDE));
        assertThatThrownBy(() -> capital.saisir(new SaisieApportCapital("Sidi", "1000", "12/11/2026"), "admin"))
                .isInstanceOf(ApportCapitalInvalideException.class);
        assertThatThrownBy(() -> capital.saisir(new SaisieApportCapital("Sidi", "1000", "2026-11-13"), "admin"))
                .isInstanceOf(ApportCapitalInvalideException.class);

        assertThat(capital.tous()).isEmpty();
        assertThat(ecritures.toutes()).isEmpty();
    }

    @Test
    void le_dossier_de_l_exemple_chiffre_engage_10_080_puis_rapporte_2_400() {
        capital.saisir(new SaisieApportCapital("Sidi", "100000", "2026-11-01"), "admin");
        Contrat c = contrat();

        comptabilite.apportEncaisse(UUID.randomUUID(), c.demandeId(), c.id(), c.numero(), c.apport(), AUJOURDHUI);
        assertThat(indicateurs.indicateurs()).as("apport reçu, fournisseur pas encore payé")
                .isEqualTo(chiffres(100_000, 0, 0, 0));

        comptabilite.paiementFournisseur(UUID.randomUUID(), "BC-2026-000001", c.demandeId(), c.id(),
                c.prix().prixAchat(), MAINTENANT);
        assertThat(indicateurs.indicateurs()).as("fournisseur payé")
                .isEqualTo(chiffres(100_000, 10_080, 12_480, 0));

        comptabilite.venteLivree(c.demandeId(), MAINTENANT);
        assertThat(indicateurs.indicateurs()).as("produit remis").isEqualTo(chiffres(100_000, 10_080, 12_480, 0));

        for (Echeance e : c.echeances()) {
            comptabilite.echeanceEncaissee(UUID.randomUUID(), c.demandeId(), c.id(), c.numero(), e.rang(),
                    e.montant(), e.date());
            if (e.rang() == 4) {
                assertThat(indicateurs.indicateurs()).as("4 échéances reçues : 8 320 + 8 320 = 16 640")
                        .isEqualTo(chiffres(100_000, 1_760, 4_160, 0));
            }
            if (e.rang() == 5) {
                assertThat(indicateurs.indicateurs()).as("5 échéances reçues : 18 720 > 18 400")
                        .isEqualTo(chiffres(100_000, 0, 2_080, 320));
            }
        }
        assertThat(indicateurs.indicateurs()).as("dossier soldé").isEqualTo(chiffres(100_000, 0, 0, 2_400));
    }

    @Test
    void un_paiement_fournisseur_contrepasse_ne_compte_plus() {
        Contrat c = contrat();
        comptabilite.apportEncaisse(UUID.randomUUID(), c.demandeId(), c.id(), c.numero(), c.apport(), AUJOURDHUI);
        UUID commande = UUID.randomUUID();
        comptabilite.paiementFournisseur(commande, "BC-2026-000001", c.demandeId(), c.id(), c.prix().prixAchat(),
                MAINTENANT);

        comptabilite.contrepasser(ecritures.parSource(NatureEcriture.PAIEMENT_FOURNISSEUR, commande).orElseThrow()
                .id(), "erreur de saisie", "admin");

        assertThat(indicateurs.indicateurs()).isEqualTo(chiffres(0, 0, 0, 0));
    }
}
