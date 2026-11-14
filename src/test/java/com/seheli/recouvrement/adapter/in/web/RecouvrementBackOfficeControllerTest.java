package com.seheli.recouvrement.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.contrat.domain.Contrat;
import com.seheli.demande.domain.MotifRecouvrementInvalideException;
import com.seheli.demande.domain.RegularisationImpossibleException;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.recouvrement.application.RappelsDeTest;
import com.seheli.recouvrement.domain.NoteInvalideException;

/** Depuis l'écran du recouvrement, un dossier en retard est régularisé ou passé en recouvrement. */
class RecouvrementBackOfficeControllerTest {

    private static final Principal AGENT = () -> "recouvrement";
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 12);

    private final RappelsDeTest r = new RappelsDeTest();
    private final RecouvrementBackOfficeController controller =
            new RecouvrementBackOfficeController(r.suivi, r.remboursements, r.clock);
    private final RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

    private Contrat enRetard() {
        Contrat c = r.contrat(r.client("36000001"), StatutDossier.EN_REMBOURSEMENT, AUJOURDHUI.minusDays(3));
        r.retards.echeancesImpayees().forEach(r.retards::constaterRetard);
        return c;
    }

    private StatutDossier statut(Contrat c) {
        return r.demandes.parId(c.demandeId()).orElseThrow().statut();
    }

    @Test
    void le_tableau_liste_le_prelevement_en_echec_avec_un_lien_vers_sa_fiche() {
        Contrat c = enRetard();
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.tableau(model)).isEqualTo("bo/recouvrement/tableau");

        @SuppressWarnings("unchecked")
        List<VuePrelevement> enEchec = (List<VuePrelevement>) model.get("enEchec");
        assertThat(enEchec).singleElement().satisfies(p -> {
            assertThat(p.demandeId()).isEqualTo(c.demandeId());
            assertThat(p.date()).isEqualTo("09/11/2026");
            assertThat(p.statutDossier()).isEqualTo(StatutDossier.EN_RETARD);
        });
        assertThat(model.get("aVenir")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).hasSize(1);
    }

    @Test
    void un_dossier_en_retard_est_regularise_depuis_l_ecran_une_fois_l_echeance_payee() {
        Contrat c = enRetard();

        String vue = controller.regulariser(c.demandeId(), AGENT, redirection);
        assertThat(vue).isEqualTo("redirect:/bo/recouvrement/dossiers/" + c.demandeId());
        assertThat(redirection.getFlashAttributes().get("erreur")).isEqualTo(RegularisationImpossibleException.CODE);
        assertThat(statut(c)).isEqualTo(StatutDossier.EN_RETARD);

        r.contrats.enregistrerStatut(c.id(), c.echeance(1).orElseThrow().enRetard().payee());
        RedirectAttributesModelMap apres = new RedirectAttributesModelMap();
        controller.regulariser(c.demandeId(), AGENT, apres);

        assertThat(apres.getFlashAttributes().get("succes")).isEqualTo("bo.recouvrement.regularise");
        assertThat(statut(c)).isEqualTo(StatutDossier.EN_REMBOURSEMENT);
    }

    @Test
    void un_dossier_en_retard_passe_en_recouvrement_depuis_l_ecran() {
        Contrat c = enRetard();

        controller.passerEnRecouvrement(c.demandeId(), " ", AGENT, redirection);
        assertThat(redirection.getFlashAttributes().get("erreur")).isEqualTo(MotifRecouvrementInvalideException.CODE);

        RedirectAttributesModelMap apres = new RedirectAttributesModelMap();
        controller.passerEnRecouvrement(c.demandeId(), "Injoignable, employeur prévenu", AGENT, apres);

        assertThat(apres.getFlashAttributes().get("succes")).isEqualTo("bo.recouvrement.en-recouvrement");
        assertThat(statut(c)).isEqualTo(StatutDossier.RECOUVREMENT);
        assertThat(r.journal.entrees()).last().satisfies(e -> {
            assertThat(e.acteur().identifiant()).isEqualTo("recouvrement");
            assertThat(e.apres()).hasValue("RECOUVREMENT");
        });
    }

    @Test
    void la_fiche_montre_les_notes_datees_et_les_decisions_possibles() {
        Contrat c = enRetard();
        controller.noter(c.demandeId(), "Appel : paiera le 15", AGENT, redirection);
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("bo.recouvrement.note.ajoutee");
        RedirectAttributesModelMap vide = new RedirectAttributesModelMap();
        controller.noter(c.demandeId(), "", AGENT, vide);
        assertThat(vide.getFlashAttributes().get("erreur")).isEqualTo(NoteInvalideException.CODE);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.fiche(c.demandeId(), model)).isEqualTo("bo/recouvrement/fiche");

        VueFicheRecouvrement f = (VueFicheRecouvrement) model.get("fiche");
        assertThat(f.notes()).singleElement().satisfies(n -> {
            assertThat(n.texte()).isEqualTo("Appel : paiera le 15");
            assertThat(n.ecriteLe()).isEqualTo("12/11/2026 09:00");
            assertThat(n.auteur()).isEqualTo("recouvrement");
        });
        assertThat(f.restantDu().valeur()).isEqualByComparingTo("12480");
        assertThat(f.echeancesEnRetard()).isEqualTo(1);
        assertThat(f.peutPasserEnRecouvrement()).isTrue();
        assertThat(f.peutRegulariser()).isFalse();
        assertThat(f.telephone()).hasValue("36000001");
    }

    @Test
    void dossier_inconnu_404() {
        assertThatThrownBy(() -> controller.fiche(UUID.randomUUID(), new ExtendedModelMap()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.regulariser(UUID.randomUUID(), AGENT, redirection))
                .isInstanceOf(ResponseStatusException.class);
    }
}
