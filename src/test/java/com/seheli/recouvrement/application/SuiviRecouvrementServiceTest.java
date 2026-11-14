package com.seheli.recouvrement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.audit.domain.EntreeJournal;
import com.seheli.contrat.domain.Contrat;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.application.DossierIntrouvableException;
import com.seheli.demande.domain.RegularisationImpossibleException;
import com.seheli.demande.domain.SoldeImpossibleException;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.recouvrement.domain.NoteInvalideException;
import com.seheli.recouvrement.domain.TypeRelance;

class SuiviRecouvrementServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 12);
    private final RappelsDeTest r = new RappelsDeTest();

    private void payer(Contrat c, int rang) {
        r.contrats.enregistrerStatut(c.id(), r.contrats.parId(c.id()).orElseThrow().echeance(rang).orElseThrow().payee());
    }

    /** Dossier EN_RETARD : première échéance (09/11/2026) impayée et constatée EN_RETARD. */
    private Contrat enRetard() {
        Contrat c = r.contrat(r.client("36000001"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.minusDays(3));
        r.retards.echeancesImpayees().stream().filter(e -> e.contratId().equals(c.id()))
                .forEach(r.retards::constaterRetard);
        return c;
    }

    @Test
    void le_tableau_separe_les_prelevements_a_venir_reussis_et_en_echec() {
        Contrat retard = enRetard();
        Contrat aVenir = r.contrat(r.client("36000002"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.plusDays(8));
        r.contrat(r.client("36000003"), StatutDossier.ANNULEE, AUJOURDHUI.plusDays(3));
        Contrat paye = r.contrat(r.client("36000004"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.minusMonths(1));
        payer(paye, 1);

        TableauRecouvrement t = r.suivi.tableau();

        assertThat(t.enEchec()).extracting(p -> p.echeance().contratId(), p -> p.echeance().echeance().rang(),
                PrelevementSuivi::statutDossier).containsExactly(tuple(retard.id(), 1, StatutDossier.EN_RETARD));
        assertThat(t.aVenir()).extracting(p -> p.echeance().contratId(), p -> p.echeance().echeance().rang())
                .containsExactly(tuple(paye.id(), 2), tuple(aVenir.id(), 1), tuple(retard.id(), 2), tuple(paye.id(), 3));
        assertThat(t.reussis()).extracting(p -> p.echeance().contratId(), p -> p.echeance().echeance().rang())
                .containsExactly(tuple(paye.id(), 1));
        assertThat(t.aVenirJusquAu()).isEqualTo(AUJOURDHUI.plusDays(30));
        assertThat(t.reussisDepuis()).isEqualTo(AUJOURDHUI.minusDays(60));
    }

    @Test
    void la_fiche_montre_l_echeancier_les_relances_les_notes_et_les_decisions_possibles() {
        Contrat c = enRetard();
        r.retards.relancesDues().forEach(r.retards::relancer);
        r.suivi.noter(c.demandeId(), "recouvrement", "Client joint, paiera vendredi.");

        FicheRecouvrement f = r.suivi.fiche(c.demandeId());

        assertThat(f.statut()).isEqualTo(StatutDossier.EN_RETARD);
        assertThat(f.contrat().id()).isEqualTo(c.id());
        assertThat(f.client()).hasValueSatisfying(cl -> assertThat(cl.telephone()).isEqualTo("36000001"));
        assertThat(f.situation().enRetard()).isEqualTo(1);
        assertThat(f.relances()).extracting(rel -> rel.type()).containsExactly(TypeRelance.ECHEC_PREMIERE);
        assertThat(f.notes()).singleElement().satisfies(n -> {
            assertThat(n.texte()).isEqualTo("Client joint, paiera vendredi.");
            assertThat(n.auteur()).isEqualTo("recouvrement");
            assertThat(n.ecriteLe()).isEqualTo(RappelsDeTest.AUJOURDHUI);
        });
        assertThat(f.peutRegulariser()).isFalse();
        assertThat(f.peutPasserEnRecouvrement()).isTrue();
        assertThat(f.peutSolder()).isFalse();
    }

    @Test
    void un_dossier_en_retard_est_regularise_une_fois_l_echeance_payee() {
        Contrat c = enRetard();
        int avant = r.journal.entrees().size();
        r.evenements.clear();

        assertThatThrownBy(() -> r.remboursements.regulariser(c.demandeId(), "recouvrement"))
                .isInstanceOf(RegularisationImpossibleException.class);
        payer(c, 1);
        assertThat(r.suivi.fiche(c.demandeId()).peutRegulariser()).isTrue();
        r.remboursements.regulariser(c.demandeId(), "recouvrement");

        assertThat(r.demandes.parId(c.demandeId()).orElseThrow().statut()).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
        assertThat(r.journal.entrees()).hasSize(avant + 1).last().satisfies(e -> {
            assertThat(e.action()).isEqualTo("REGULARISATION");
            assertThat(e.acteur().identifiant()).isEqualTo("recouvrement");
            assertThat(e.avant()).hasValue("EN_RETARD");
            assertThat(e.apres()).hasValue("EN_REMBOURSEMENT");
        });
        assertThat(r.evenements).singleElement().isInstanceOfSatisfying(StatutDossierChange.class,
                e -> assertThat(e.vers()).isEqualTo(StatutDossier.EN_REMBOURSEMENT));
        assertThat(r.retards.echeancesEnRetard()).isEmpty();
    }

    @Test
    void un_dossier_en_retard_passe_en_recouvrement_avec_son_motif_au_journal() {
        Contrat c = enRetard();

        r.remboursements.passerEnRecouvrement(c.demandeId(), "recouvrement", "  Injoignable depuis 3 semaines ");

        assertThat(r.demandes.parId(c.demandeId()).orElseThrow().statut()).isEqualTo(StatutDossier.RECOUVREMENT);
        assertThat(r.journal.entrees()).last().satisfies(e -> {
            assertThat(e.action()).isEqualTo("PASSAGE_EN_RECOUVREMENT");
            assertThat(e.detail()).hasValue("Contrat " + c.numero() + " : Injoignable depuis 3 semaines");
        });
        assertThat(r.suivi.fiche(c.demandeId()).peutPasserEnRecouvrement()).isFalse();
    }

    @Test
    void un_dossier_entierement_paye_est_solde() {
        Contrat c = r.contrat(r.client("36000001"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.plusDays(1));
        for (int rang = 1; rang <= 5; rang++) {
            payer(c, rang);
        }
        assertThatThrownBy(() -> r.remboursements.solder(c.demandeId(), "recouvrement"))
                .isInstanceOf(SoldeImpossibleException.class);
        payer(c, 6);
        assertThat(r.suivi.fiche(c.demandeId()).peutSolder()).isTrue();

        r.remboursements.solder(c.demandeId(), "recouvrement");

        assertThat(r.demandes.parId(c.demandeId()).orElseThrow().statut()).isEqualTo(StatutDossier.SOLDEE);
        assertThat(r.journal.entrees()).last().extracting(EntreeJournal::action).isEqualTo("SOLDE");
    }

    @Test
    void note_vide_refusee_et_dossier_inconnu_introuvable() {
        Contrat c = enRetard();

        assertThatThrownBy(() -> r.suivi.noter(c.demandeId(), "recouvrement", " "))
                .isInstanceOf(NoteInvalideException.class);
        assertThatThrownBy(() -> r.suivi.noter(UUID.randomUUID(), "recouvrement", "Note"))
                .isInstanceOf(DossierIntrouvableException.class);
        assertThatThrownBy(() -> r.suivi.fiche(UUID.randomUUID())).isInstanceOf(DossierIntrouvableException.class);
        assertThatThrownBy(() -> r.remboursements.regulariser(UUID.randomUUID(), "recouvrement"))
                .isInstanceOf(DossierIntrouvableException.class);
    }
}
