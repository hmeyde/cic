package com.seheli.comptabilite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;

class EcritureComptableTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 11, 13);
    private static final Instant LE = Instant.parse("2026-11-13T10:00:00Z");
    private static final UUID DOSSIER = UUID.randomUUID();
    private static final UUID CONTRAT = UUID.randomUUID();
    private static final String NUMERO = "SEH-2026-000001";

    /** Solde débiteur (positif) ou créditeur (négatif) de chaque compte, en ouguiyas. */
    private static Map<Compte, Long> soldes(List<EcritureComptable> ecritures) {
        Map<Compte, Long> soldes = new EnumMap<>(Compte.class);
        for (EcritureComptable e : ecritures) {
            for (LigneEcriture l : e.lignes()) {
                long m = l.montant().valeur().longValueExact();
                soldes.merge(l.compte(), l.sens() == Sens.DEBIT ? m : -m, Long::sum);
            }
        }
        return soldes;
    }

    @Test
    void exemple_chiffre_un_dossier_rembourse_laisse_le_benefice_brut_en_banque() {
        List<EcritureComptable> ecritures = new java.util.ArrayList<>();
        ecritures.add(EcritureComptable.apportEncaisse(UUID.randomUUID(), DOSSIER, CONTRAT, NUMERO, Montant.de(8_320),
                JOUR, LE));
        ecritures.add(EcritureComptable.paiementFournisseur(UUID.randomUUID(), "BC-2026-000001", DOSSIER, CONTRAT,
                Montant.de(18_400), JOUR, LE));
        ecritures.add(EcritureComptable.venteLivree(DOSSIER, CONTRAT, NUMERO, Montant.de(20_800), Montant.de(8_320),
                JOUR, LE));
        for (int rang = 1; rang <= 6; rang++) {
            ecritures.add(EcritureComptable.echeanceEncaissee(UUID.randomUUID(), DOSSIER, CONTRAT, NUMERO, rang,
                    Montant.de(2_080), JOUR.plusMonths(rang), LE));
        }

        Map<Compte, Long> soldes = soldes(ecritures);

        assertThat(soldes).containsEntry(Compte.BANQUE, 2_400L).containsEntry(Compte.CLIENTS, 0L)
                .containsEntry(Compte.CLIENTS_APPORTS, 0L).containsEntry(Compte.ACHATS, 18_400L)
                .containsEntry(Compte.VENTES, -20_800L);
        assertThat(soldes.values().stream().mapToLong(Long::longValue).sum()).isZero();
        assertThat(ecritures).allSatisfy(e -> assertThat(e.total(Sens.DEBIT)).isEqualTo(e.total(Sens.CREDIT)));
    }

    @Test
    void apport_encaisse_debite_la_banque_et_credite_les_apports_clients() {
        UUID paiement = UUID.randomUUID();

        EcritureComptable e = EcritureComptable.apportEncaisse(paiement, DOSSIER, CONTRAT, NUMERO, Montant.de(8_320),
                JOUR, LE);

        assertThat(e.nature()).isEqualTo(NatureEcriture.APPORT_ENCAISSE);
        assertThat(e.sourceId()).isEqualTo(paiement);
        assertThat(e.date()).isEqualTo(JOUR);
        assertThat(e.demandeId()).contains(DOSSIER);
        assertThat(e.libelle()).isEqualTo("Apport du contrat SEH-2026-000001");
        assertThat(e.lignes()).containsExactly(LigneEcriture.debit(Compte.BANQUE, Montant.de(8_320)),
                LigneEcriture.credit(Compte.CLIENTS_APPORTS, Montant.de(8_320)));
        assertThat(e.montant()).isEqualTo(Montant.de(8_320));
    }

    @Test
    void vente_livree_solde_l_apport_et_ouvre_le_reste_du() {
        EcritureComptable e = EcritureComptable.venteLivree(DOSSIER, CONTRAT, NUMERO, Montant.de(20_800),
                Montant.de(8_320), JOUR, LE);

        assertThat(e.sourceId()).isEqualTo(DOSSIER);
        assertThat(e.lignes()).containsExactly(LigneEcriture.debit(Compte.CLIENTS_APPORTS, Montant.de(8_320)),
                LigneEcriture.debit(Compte.CLIENTS, Montant.de(12_480)),
                LigneEcriture.credit(Compte.VENTES, Montant.de(20_800)));
    }

    @Test
    void paiement_a_identifier_en_attente_sans_dossier() {
        EcritureComptable e = EcritureComptable.paiementAIdentifier(UUID.randomUUID(), Montant.de(500), JOUR, LE);

        assertThat(e.demandeId()).isEmpty();
        assertThat(e.lignes()).containsExactly(LigneEcriture.debit(Compte.BANQUE, Montant.de(500)),
                LigneEcriture.credit(Compte.PAIEMENTS_A_IDENTIFIER, Montant.de(500)));
    }

    @Test
    void une_ecriture_desequilibree_ou_a_une_seule_ligne_est_refusee() {
        assertThatThrownBy(() -> EcritureComptable.reconstituer(UUID.randomUUID(), NatureEcriture.APPORT_ENCAISSE,
                UUID.randomUUID(), JOUR, "Apport", null, null,
                List.of(LigneEcriture.debit(Compte.BANQUE, Montant.de(100)),
                        LigneEcriture.credit(Compte.CLIENTS_APPORTS, Montant.de(99))),
                LE)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("déséquilibrée");
        assertThatThrownBy(() -> EcritureComptable.reconstituer(UUID.randomUUID(), NatureEcriture.APPORT_ENCAISSE,
                UUID.randomUUID(), JOUR, "Apport", null, null, List.of(LigneEcriture.debit(Compte.BANQUE,
                        Montant.de(100))), LE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LigneEcriture.debit(Compte.BANQUE, Montant.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void la_contrepassation_inverse_chaque_ligne_et_annule_l_ecriture() {
        EcritureComptable origine = EcritureComptable.paiementFournisseur(UUID.randomUUID(), "BC-2026-000001", DOSSIER,
                CONTRAT, Montant.de(18_400), JOUR, LE);

        EcritureComptable inverse = origine.contrepasser("montant erroné", JOUR.plusDays(1), LE.plusSeconds(60));

        assertThat(inverse.nature()).isEqualTo(NatureEcriture.CONTREPASSATION);
        assertThat(inverse.contrepasse()).contains(origine.id());
        assertThat(inverse.demandeId()).contains(DOSSIER);
        assertThat(inverse.libelle()).startsWith("Contrepassation : Paiement fournisseur").endsWith("montant erroné");
        assertThat(inverse.lignes()).containsExactly(LigneEcriture.credit(Compte.ACHATS, Montant.de(18_400)),
                LigneEcriture.debit(Compte.BANQUE, Montant.de(18_400)));
        assertThat(soldes(List.of(origine, inverse)).values()).containsOnly(0L);
        assertThat(origine.lignes()).first().isEqualTo(LigneEcriture.debit(Compte.ACHATS, Montant.de(18_400)));
        assertThatThrownBy(() -> inverse.contrepasser("", JOUR, LE))
                .isInstanceOf(EcritureDejaContrepasseeException.class);
    }

    @Test
    void les_comptes_se_retrouvent_par_leur_numero() {
        assertThat(Compte.deNumero("512")).isEqualTo(Compte.BANQUE);
        assertThat(Compte.deNumero("4191")).isEqualTo(Compte.CLIENTS_APPORTS);
        assertThatThrownBy(() -> Compte.deNumero("999")).isInstanceOf(IllegalArgumentException.class);
    }
}
