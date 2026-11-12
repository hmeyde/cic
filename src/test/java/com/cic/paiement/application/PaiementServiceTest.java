package com.cic.paiement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.audit.domain.EntreeJournal;
import com.cic.audit.domain.TypeActeur;
import com.cic.commun.domain.Montant;
import com.cic.contrat.application.AutorisationsEnMemoire;
import com.cic.contrat.application.ContratAEtablir;
import com.cic.contrat.application.ContratService;
import com.cic.contrat.application.ContratsEnMemoire;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Prix;
import com.cic.paiement.PaiementRapproche;
import com.cic.paiement.domain.Paiement;
import com.cic.paiement.domain.PaiementEnDoubleException;
import com.cic.paiement.domain.ReferenceApport;
import com.cic.paiement.domain.StatutRapprochement;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class PaiementServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant MAINTENANT = Instant.parse("2026-11-14T10:00:00Z");

    private final ContratsEnMemoire contrats = new ContratsEnMemoire();
    private final PaiementsEnMemoire paiements = new PaiementsEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(new JournalEnMemoire());
    private final List<Object> evenements = new ArrayList<>();
    private final Clock clock = Clock.fixed(MAINTENANT, NOUAKCHOTT);
    private final ContratService contratService;
    private final PaiementService service;

    PaiementServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        contratService = new ContratService(contrats, new AutorisationsEnMemoire(), parametres, clock);
        service = new PaiementService(paiements, contratService, journal, evenements::add, clock);
    }

    /** Contrat de l'exemple chiffré : apport 8 320 MRU. */
    private Contrat contrat() {
        UUID demandeId = UUID.randomUUID();
        return contratService.etablir(new ContratAEtablir(demandeId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), 6, 25, Optional.empty(), MAINTENANT.minusSeconds(86_400)));
    }

    private static String reference(Contrat c) {
        return ReferenceApport.pourContrat(c.numero()).valeur();
    }

    private List<EntreeJournal> journalDe(Paiement p) {
        return journal.historique(PaiementService.objet(p.id()));
    }

    @Test
    void un_paiement_saisi_est_rattache_au_dossier_de_sa_reference() {
        Contrat premier = contrat();
        Contrat second = contrat();

        Paiement p = service.saisir(new SaisiePaiement(reference(second), "8 320", "2026-11-13"), "admin");

        assertThat(p.statut()).isEqualTo(StatutRapprochement.RAPPROCHE);
        assertThat(p.apportAttendu()).hasValueSatisfying(a -> {
            assertThat(a.demandeId()).isEqualTo(second.demandeId()).isNotEqualTo(premier.demandeId());
            assertThat(a.contratId()).isEqualTo(second.id());
            assertThat(a.clientId()).isEqualTo(second.clientId());
            assertThat(a.numeroContrat()).isEqualTo(second.numero());
            assertThat(a.montant()).isEqualTo(Montant.de(8_320));
        });
        assertThat(p.ecartDeMontant()).isFalse();
        assertThat(p.saisiPar()).isEqualTo("admin");
        assertThat(p.saisiLe()).isEqualTo(MAINTENANT);
        assertThat(paiements.tous()).containsExactly(p);
        assertThat(evenements).singleElement().isInstanceOfSatisfying(PaiementRapproche.class, e -> {
            assertThat(e.paiementId()).isEqualTo(p.id());
            assertThat(e.demandeId()).isEqualTo(second.demandeId());
            assertThat(e.contratId()).isEqualTo(second.id());
            assertThat(e.apportSolde()).isTrue();
        });
        assertThat(journalDe(p)).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo(PaiementService.ACTION_SAISIE);
            assertThat(e.acteur().type()).isEqualTo(TypeActeur.BACK_OFFICE);
            assertThat(e.apres()).contains("RAPPROCHE");
            assertThat(e.detail()).hasValueSatisfying(d -> assertThat(d).contains(second.numero()));
        });
    }

    @Test
    void la_reference_saisie_est_normalisee_avant_le_rapprochement() {
        Contrat c = contrat();
        String saisie = " " + reference(c).toLowerCase().substring(0, 6) + " " + reference(c).substring(6) + " ";

        Paiement p = service.saisir(new SaisiePaiement(saisie, "8320", "2026-11-14"), "admin");

        assertThat(p.reference().valeur()).isEqualTo(reference(c));
        assertThat(p.estRapproche()).isTrue();
    }

    @Test
    void un_doublon_est_refuse_sans_rien_enregistrer() {
        Contrat c = contrat();
        Paiement premier = service.saisir(new SaisiePaiement(reference(c), "8320", "2026-11-13"), "admin");

        assertThatThrownBy(() -> service.saisir(new SaisiePaiement(reference(c), "8320", "2026-11-13"), "recouvrement"))
                .isInstanceOfSatisfying(PaiementEnDoubleException.class, e -> {
                    assertThat(e.code()).isEqualTo(PaiementEnDoubleException.CODE);
                    assertThat(e.paiementExistantId()).contains(premier.id());
                });

        assertThat(paiements.tous()).containsExactly(premier);
        assertThat(evenements).hasSize(1);
        assertThat(journalDe(premier)).hasSize(1);
    }

    @Test
    void meme_reference_avec_un_autre_montant_ou_une_autre_ecriture_refusee_aussi() {
        Contrat c = contrat();
        service.saisir(new SaisiePaiement(reference(c), "8320", "2026-11-13"), "admin");

        assertThatThrownBy(() -> service.saisir(new SaisiePaiement(reference(c), "5000", "2026-11-14"), "admin"))
                .isInstanceOf(PaiementEnDoubleException.class);
        assertThatThrownBy(() -> service.saisir(
                new SaisiePaiement(reference(c).toLowerCase(), "8320", "2026-11-13"), "admin"))
                .isInstanceOf(PaiementEnDoubleException.class);
        assertThat(paiements.tous()).hasSize(1);
    }

    @Test
    void ecart_de_montant_signale_mais_paiement_rattache() {
        Contrat c = contrat();

        Paiement p = service.saisir(new SaisiePaiement(reference(c), "8000", "2026-11-13"), "admin");

        assertThat(p.estRapproche()).isTrue();
        assertThat(p.ecartDeMontant()).isTrue();
        assertThat(evenements).singleElement().isInstanceOfSatisfying(PaiementRapproche.class,
                e -> assertThat(e.apportSolde()).isFalse());
    }

    @Test
    void reference_sans_dossier_enregistree_non_rapprochee() {
        contrat();
        String fauteDeFrappe = ReferenceApport.pourContrat("CIC-2026-000001").valeur().replace("AP2026", "AP2062");
        String inconnue = ReferenceApport.pourContrat("CIC-2026-000999").valeur();

        Paiement p1 = service.saisir(new SaisiePaiement(fauteDeFrappe, "8320", "2026-11-13"), "admin");
        Paiement p2 = service.saisir(new SaisiePaiement(inconnue, "8320", "2026-11-13"), "admin");
        Paiement p3 = service.saisir(new SaisiePaiement("TRX42", "8320", "2026-11-13"), "admin");

        assertThat(List.of(p1, p2, p3)).allSatisfy(p -> {
            assertThat(p.statut()).isEqualTo(StatutRapprochement.NON_RAPPROCHE);
            assertThat(p.apportAttendu()).isEmpty();
        });
        assertThat(paiements.tous()).hasSize(3);
        assertThat(evenements).isEmpty();
        assertThat(journalDe(p1)).singleElement().satisfies(e -> assertThat(e.apres()).contains("NON_RAPPROCHE"));
    }

    @Test
    void saisie_invalide_refusee() {
        Contrat c = contrat();
        String ref = reference(c);

        verifierRefus(new SaisiePaiement("", "8320", "2026-11-13"), SaisiePaiementInvalideException.REFERENCE_INVALIDE);
        verifierRefus(new SaisiePaiement(null, "8320", "2026-11-13"), SaisiePaiementInvalideException.REFERENCE_INVALIDE);
        verifierRefus(new SaisiePaiement("AP/1", "8320", "2026-11-13"), SaisiePaiementInvalideException.REFERENCE_INVALIDE);
        verifierRefus(new SaisiePaiement(ref, "0", "2026-11-13"), SaisiePaiementInvalideException.MONTANT_INVALIDE);
        verifierRefus(new SaisiePaiement(ref, "8320.50", "2026-11-13"), SaisiePaiementInvalideException.MONTANT_INVALIDE);
        verifierRefus(new SaisiePaiement(ref, "-8320", "2026-11-13"), SaisiePaiementInvalideException.MONTANT_INVALIDE);
        verifierRefus(new SaisiePaiement(ref, "", "2026-11-13"), SaisiePaiementInvalideException.MONTANT_INVALIDE);
        verifierRefus(new SaisiePaiement(ref, "8320", ""), SaisiePaiementInvalideException.DATE_INVALIDE);
        verifierRefus(new SaisiePaiement(ref, "8320", "13/11/2026"), SaisiePaiementInvalideException.DATE_INVALIDE);
        verifierRefus(new SaisiePaiement(ref, "8320", "2026-11-15"), SaisiePaiementInvalideException.DATE_INVALIDE);
        assertThat(paiements.tous()).isEmpty();
        assertThat(evenements).isEmpty();
    }

    private void verifierRefus(SaisiePaiement saisie, String code) {
        assertThatThrownBy(() -> service.saisir(saisie, "admin"))
                .isInstanceOfSatisfying(SaisiePaiementInvalideException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    void derniers_paiements_du_plus_recent_au_plus_ancien() {
        Paiement p1 = service.saisir(new SaisiePaiement("TRX1", "100", "2026-11-13"), "admin");
        PaiementService plusTard = new PaiementService(paiements, contratService, journal, evenements::add,
                Clock.offset(clock, java.time.Duration.ofMinutes(5)));
        Paiement p2 = plusTard.saisir(new SaisiePaiement("TRX2", "100", "2026-11-13"), "admin");

        assertThat(service.derniers(10)).containsExactly(p2, p1);
        assertThat(service.derniers(1)).containsExactly(p2);
    }
}
