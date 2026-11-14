package com.seheli.demande.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.demande.application.DecisionsDeTest;
import com.seheli.demande.application.DemandesEnMemoire;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandesFixtures;
import com.seheli.demande.domain.MotifRefusInvalideException;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.demande.domain.TransitionInterditeException;
import com.seheli.identite.domain.TypeDocument;

class DecisionBackOfficeControllerTest {

    private static final Principal ANALYSTE = () -> "analyste";

    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final JournalEnMemoire journal = new JournalEnMemoire();
    private final DecisionsDeTest decisions = new DecisionsDeTest(demandes, new JournalAuditService(journal),
            evenement -> { }, Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC));
    private final DecisionBackOfficeController controller = new DecisionBackOfficeController(decisions.service);
    private final RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

    private Demande enAnalyse() {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), StatutDossier.EN_ANALYSE);
        demandes.enregistrer(d);
        decisions.emploiCdi(d.clientId());
        return d;
    }

    @Test
    void accepter_par_l_analyste_connecte_puis_retour_a_la_fiche() {
        Demande d = enAnalyse();

        String vue = controller.accepter(d.id(), ANALYSTE, redirection);

        assertThat(vue).isEqualTo("redirect:/bo/dossiers/" + d.id());
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("bo.decision.acceptee");
        assertThat(demandes.parId(d.id()).orElseThrow().decision())
                .hasValueSatisfying(dec -> assertThat(dec.analyste()).isEqualTo("analyste"));
        assertThat(journal.entrees()).singleElement()
                .satisfies(e -> assertThat(e.acteur().identifiant()).isEqualTo("analyste"));
    }

    @Test
    void refus_sans_motif_affiche_l_erreur_sur_la_fiche() {
        Demande d = enAnalyse();

        String vue = controller.refuser(d.id(), null, ANALYSTE, redirection);

        assertThat(vue).isEqualTo("redirect:/bo/dossiers/" + d.id());
        assertThat(redirection.getFlashAttributes().get("erreur")).isEqualTo(MotifRefusInvalideException.CODE);
        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.EN_ANALYSE);
    }

    @Test
    void decision_sur_un_dossier_deja_decide_affiche_l_erreur() {
        Demande d = enAnalyse();
        controller.accepter(d.id(), ANALYSTE, new RedirectAttributesModelMap());

        controller.refuser(d.id(), "Motif", ANALYSTE, redirection);

        assertThat(redirection.getFlashAttributes().get("erreur")).isEqualTo(TransitionInterditeException.CODE);
    }

    @Test
    void demander_un_justificatif() {
        Demande d = enAnalyse();

        controller.demanderJustificatif(d.id(), TypeDocument.PIECE_IDENTITE, "", ANALYSTE, redirection);

        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("bo.decision.justificatif-demande");
        assertThat(journal.entrees()).singleElement()
                .satisfies(e -> assertThat(e.action()).isEqualTo("DEMANDE_JUSTIFICATIF"));
    }

    @Test
    void dossier_inconnu_renvoie_404() {
        assertThatThrownBy(() -> controller.accepter(UUID.randomUUID(), ANALYSTE, redirection))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
