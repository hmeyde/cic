package com.cic.contrat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.ContratDejaEtabliException;
import com.cic.contrat.domain.Echeance;
import com.cic.contrat.domain.FinContratDepasseeException;
import com.cic.contrat.domain.Prix;
import com.cic.contrat.domain.StatutAutorisation;
import com.cic.contrat.domain.StatutEcheance;
import com.cic.contrat.domain.TypeAutorisation;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class ContratServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    /** Acceptation le 13/11/2026 : date limite de l'apport le 20/11/2026 (7 jours). */
    private static final Instant ACCEPTE_LE = Instant.parse("2026-11-13T09:30:00Z");

    private final ContratsEnMemoire contrats = new ContratsEnMemoire();
    private final AutorisationsEnMemoire autorisations = new AutorisationsEnMemoire();
    private final ContratService service;

    ContratServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        service = new ContratService(contrats, autorisations, parametres, Clock.fixed(ACCEPTE_LE, NOUAKCHOTT));
    }

    /** Exemple chiffré de CLAUDE.md : prix public 20 000, achat 18 400, vente 20 800, apport 8 320. */
    private static ContratAEtablir exemple(UUID demandeId, int jourPaie, Optional<LocalDate> finContrat) {
        return new ContratAEtablir(demandeId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), 6, jourPaie, finContrat, ACCEPTE_LE);
    }

    @Test
    void etablit_le_contrat_avec_le_prix_fixe_et_six_echeances_a_venir() {
        UUID demandeId = UUID.randomUUID();

        Contrat contrat = service.etablir(exemple(demandeId, 25, Optional.empty()));

        assertThat(contrat.numero()).isEqualTo("CIC-2026-000001");
        assertThat(contrat.demandeId()).isEqualTo(demandeId);
        assertThat(contrat.etabliLe()).isEqualTo(ACCEPTE_LE);
        assertThat(contrat.prix().prixVente()).isEqualTo(Montant.de(20_800));
        assertThat(contrat.apport()).isEqualTo(Montant.de(8_320));
        assertThat(contrat.resteAFinancer()).isEqualTo(Montant.de(12_480));
        assertThat(contrat.capitalEngage()).isEqualTo(Montant.de(10_080));
        assertThat(contrat.beneficeBrut()).isEqualTo(Montant.de(2_400));
        assertThat(contrat.echeances()).hasSize(6)
                .allSatisfy(e -> {
                    assertThat(e.montant()).isEqualTo(Montant.de(2_080));
                    assertThat(e.statut()).isEqualTo(StatutEcheance.A_VENIR);
                })
                .extracting(Echeance::date)
                .containsExactly(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 25),
                        LocalDate.of(2027, 2, 25), LocalDate.of(2027, 3, 25), LocalDate.of(2027, 4, 25));
        assertThat(service.parDemande(demandeId)).contains(contrat);
    }

    @Test
    void emet_avec_le_contrat_une_autorisation_de_prelevement_a_signer_en_virement_permanent() {
        UUID demandeId = UUID.randomUUID();

        Contrat contrat = service.etablir(exemple(demandeId, 25, Optional.empty()));

        assertThat(service.autorisation(contrat.id())).hasValueSatisfying(a -> {
            assertThat(a.contratId()).isEqualTo(contrat.id());
            assertThat(a.demandeId()).isEqualTo(demandeId);
            assertThat(a.clientId()).isEqualTo(contrat.clientId());
            assertThat(a.type()).isEqualTo(TypeAutorisation.VIREMENT_PERMANENT);
            assertThat(a.statut()).isEqualTo(StatutAutorisation.A_SIGNER);
            assertThat(a.emiseLe()).isEqualTo(ACCEPTE_LE);
        });
    }

    @Test
    void la_premiere_echeance_suit_la_date_limite_de_l_apport() {
        // Paie le 15 : le 15/11 précède la date limite du 20/11, première échéance le 15/12.
        Contrat contrat = service.etablir(exemple(UUID.randomUUID(), 15, Optional.empty()));

        assertThat(contrat.echeances().getFirst().date()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(contrat.echeances().getLast().date()).isEqualTo(LocalDate.of(2027, 5, 15));
    }

    @Test
    void la_derniere_echeance_absorbe_l_ecart_d_arrondi() {
        ContratAEtablir a = new ContratAEtablir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Réfrigérateur",
                new Prix(Montant.de(10_000), Montant.de(9_200), Montant.de(10_400), Montant.de(4_160)),
                Montant.de(4_161), 4, 25, Optional.empty(), ACCEPTE_LE);

        Contrat contrat = service.etablir(a);

        assertThat(contrat.echeances()).extracting(Echeance::montant).containsExactly(Montant.de(1_560),
                Montant.de(1_560), Montant.de(1_560), Montant.de(1_559));
    }

    @Test
    void cdd_termine_avant_la_derniere_echeance() {
        assertThatThrownBy(() -> service.etablir(exemple(UUID.randomUUID(), 25,
                Optional.of(LocalDate.of(2027, 4, 25)))))
                .isInstanceOf(FinContratDepasseeException.class);
        assertThat(contrats.tous()).isEmpty();
        assertThat(autorisations.toutes()).isEmpty();
    }

    @Test
    void un_seul_contrat_par_dossier() {
        UUID demandeId = UUID.randomUUID();
        service.etablir(exemple(demandeId, 25, Optional.empty()));

        assertThatThrownBy(() -> service.etablir(exemple(demandeId, 25, Optional.empty())))
                .isInstanceOf(ContratDejaEtabliException.class);
    }

    @Test
    void numero_lisible() {
        assertThat(ContratService.numero(LocalDate.of(2026, 11, 13), 42)).isEqualTo("CIC-2026-000042");
    }

    @Test
    void retrouve_un_contrat_par_les_chiffres_de_son_numero() {
        Contrat c = service.etablir(exemple(UUID.randomUUID(), 25, Optional.empty()));

        assertThat(service.parChiffresDuNumero("2026000001")).contains(c);
        assertThat(service.parChiffresDuNumero("2026000002")).isEmpty();
        assertThat(service.parChiffresDuNumero("202600001")).as("zéros manquants").isEmpty();
        assertThat(service.parChiffresDuNumero("20260000001")).as("zéro de trop").isEmpty();
        assertThat(service.parChiffresDuNumero("2026")).isEmpty();
        assertThat(service.parChiffresDuNumero("9".repeat(23))).isEmpty();
        assertThat(service.parChiffresDuNumero("AP2026000001")).isEmpty();
        assertThat(service.parChiffresDuNumero(null)).isEmpty();
    }
}
