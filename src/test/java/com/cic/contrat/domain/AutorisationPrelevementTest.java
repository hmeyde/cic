package com.cic.contrat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;
import com.cic.identite.domain.FormatDocument;

class AutorisationPrelevementTest {

    private static final Instant LE = Instant.parse("2026-11-13T09:30:00Z");

    private static Contrat contrat() {
        return Contrat.etablir("CIC-2026-000001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320),
                Echeancier.planifier(Montant.de(12_480), 6, LocalDate.of(2026, 11, 20), 25, Optional.empty()), LE);
    }

    @Test
    void emise_avec_le_contrat_en_virement_permanent_a_signer() {
        Contrat c = contrat();

        AutorisationPrelevement a = AutorisationPrelevement.emettre(c, LE);

        assertThat(a.contratId()).isEqualTo(c.id());
        assertThat(a.demandeId()).isEqualTo(c.demandeId());
        assertThat(a.clientId()).isEqualTo(c.clientId());
        assertThat(a.type()).isEqualTo(TypeAutorisation.VIREMENT_PERMANENT);
        assertThat(a.statut()).isEqualTo(StatutAutorisation.A_SIGNER);
        assertThat(a.emiseLe()).isEqualTo(LE);
    }

    @Test
    void le_contenu_imprime_associe_l_autorisation_a_son_propre_contrat() {
        Contrat c = contrat();
        AutorisationPrelevement autreContrat = AutorisationPrelevement.emettre(contrat(), LE);
        Acheteur acheteur = new Acheteur("Ould Ahmed", "Mohamed", "1234567890", LocalDate.of(1990, 5, 1), "22123456");
        CompteBeneficiaire cic = new CompteBeneficiaire("CIC SARL", "BMCI", "00020");

        assertThatThrownBy(() -> new ContenuAutorisation(autreContrat, c, acheteur, cic, Optional.empty(),
                LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 20)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static DocumentSigne document(Instant le) {
        return DocumentSigne.nouveau(FormatDocument.PDF, 1_000, le, "analyste");
    }

    @Test
    void l_analyste_fait_passer_l_autorisation_a_acceptee_apres_signature_et_transmission() {
        AutorisationPrelevement a = AutorisationPrelevement.emettre(contrat(), LE);
        Instant signeeLe = LE.plusSeconds(3_600);
        Instant transmiseLe = signeeLe.plusSeconds(3_600);
        Instant accepteeLe = transmiseLe.plusSeconds(86_400);

        assertThat(a.deposerDocumentSigne(document(signeeLe)))
                .contains(new ChangementStatutAutorisation(StatutAutorisation.A_SIGNER, StatutAutorisation.SIGNEE));
        assertThat(a.transmettre(transmiseLe))
                .isEqualTo(new ChangementStatutAutorisation(StatutAutorisation.SIGNEE, StatutAutorisation.TRANSMISE));
        assertThat(a.accepter(accepteeLe)).isEqualTo(
                new ChangementStatutAutorisation(StatutAutorisation.TRANSMISE, StatutAutorisation.ACCEPTEE));

        assertThat(a.statut()).isEqualTo(StatutAutorisation.ACCEPTEE);
        assertThat(a.statutChangeLe()).isEqualTo(accepteeLe);
        assertThat(a.documentSigne()).isPresent();
    }

    @Test
    void une_autorisation_non_signee_ne_peut_etre_ni_transmise_ni_acceptee() {
        AutorisationPrelevement a = AutorisationPrelevement.emettre(contrat(), LE);

        assertThatThrownBy(() -> a.transmettre(LE)).isInstanceOf(TransitionAutorisationInterditeException.class);
        assertThatThrownBy(() -> a.accepter(LE)).isInstanceOf(TransitionAutorisationInterditeException.class)
                .extracting(e -> ((TransitionAutorisationInterditeException) e).code())
                .isEqualTo(TransitionAutorisationInterditeException.CODE);
        assertThatThrownBy(() -> a.revoquer(LE)).isInstanceOf(TransitionAutorisationInterditeException.class);
        assertThat(a.statut()).isEqualTo(StatutAutorisation.A_SIGNER);
        assertThat(a.statutChangeLe()).isEqualTo(LE);
    }

    @Test
    void une_autorisation_signee_ne_peut_pas_etre_acceptee_sans_transmission() {
        AutorisationPrelevement a = AutorisationPrelevement.emettre(contrat(), LE);
        a.deposerDocumentSigne(document(LE));

        assertThatThrownBy(() -> a.accepter(LE)).isInstanceOf(TransitionAutorisationInterditeException.class);
        assertThat(a.statut()).isEqualTo(StatutAutorisation.SIGNEE);
    }

    @Test
    void le_document_d_une_autorisation_signee_peut_etre_remplace_sans_changer_de_statut() {
        AutorisationPrelevement a = AutorisationPrelevement.emettre(contrat(), LE);
        a.deposerDocumentSigne(document(LE));
        DocumentSigne nouveau = document(LE.plusSeconds(60));

        assertThat(a.deposerDocumentSigne(nouveau)).isEmpty();

        assertThat(a.statut()).isEqualTo(StatutAutorisation.SIGNEE);
        assertThat(a.documentSigne()).contains(nouveau);
    }

    @Test
    void le_document_ne_change_plus_apres_la_transmission() {
        AutorisationPrelevement a = AutorisationPrelevement.emettre(contrat(), LE);
        DocumentSigne premier = document(LE);
        a.deposerDocumentSigne(premier);
        a.transmettre(LE);

        assertThatThrownBy(() -> a.deposerDocumentSigne(document(LE)))
                .isInstanceOf(DepotDocumentSigneImpossibleException.class);
        assertThat(a.documentSigne()).contains(premier);
    }

    @Test
    void une_autorisation_revoquee_n_evolue_plus() {
        AutorisationPrelevement a = AutorisationPrelevement.emettre(contrat(), LE);
        a.deposerDocumentSigne(document(LE));
        a.transmettre(LE);
        a.accepter(LE);
        a.activer(LE);

        a.revoquer(LE);

        assertThat(a.statut()).isEqualTo(StatutAutorisation.REVOQUEE);
        assertThatThrownBy(() -> a.activer(LE)).isInstanceOf(TransitionAutorisationInterditeException.class);
        assertThatThrownBy(() -> a.deposerDocumentSigne(document(LE)))
                .isInstanceOf(DepotDocumentSigneImpossibleException.class);
    }

    @Test
    void le_document_signe_existe_si_et_seulement_si_l_autorisation_n_est_plus_a_signer() {
        Contrat c = contrat();

        assertThatThrownBy(() -> AutorisationPrelevement.reconstituer(UUID.randomUUID(), c.id(), c.demandeId(),
                c.clientId(), TypeAutorisation.VIREMENT_PERMANENT, StatutAutorisation.SIGNEE, LE, LE, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AutorisationPrelevement.reconstituer(UUID.randomUUID(), c.id(), c.demandeId(),
                c.clientId(), TypeAutorisation.VIREMENT_PERMANENT, StatutAutorisation.A_SIGNER, LE, LE,
                document(LE))).isInstanceOf(IllegalArgumentException.class);
    }
}
