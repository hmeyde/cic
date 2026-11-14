package com.seheli.demande.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.audit.domain.TypeActeur;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandesFixtures;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;

class ExpirationDossiersServiceTest {

    /** Les dossiers de la fixture sont ACCEPTEE depuis {@link DemandesFixtures#LE}. */
    private static final Instant HUIT_JOURS_APRES = DemandesFixtures.LE.plus(Duration.ofDays(8));

    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final JournalEnMemoire entrees = new JournalEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(entrees);
    private final List<Object> evenements = new ArrayList<>();

    private ExpirationDossiersService service(Instant maintenant, ParametreService parametres) {
        return new ExpirationDossiersService(demandes, parametres, journal, evenements::add,
                Clock.fixed(maintenant, ZoneOffset.UTC));
    }

    private ExpirationDossiersService service(Instant maintenant) {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        return service(maintenant, parametres);
    }

    private Demande enregistree(StatutDossier statut) {
        Demande d = DemandesFixtures.demande(UUID.randomUUID(), statut);
        demandes.enregistrer(d);
        return d;
    }

    @Test
    void un_dossier_expire_passe_en_annulee_avec_journal_et_evenement() {
        Demande d = enregistree(StatutDossier.ACCEPTEE);
        ExpirationDossiersService service = service(HUIT_JOURS_APRES);

        assertThat(service.dossiersADelaiDepasse()).containsExactly(d.id());
        assertThat(service.annulerSiDelaiDepasse(d.id())).isTrue();

        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.ANNULEE);
        assertThat(demandes.verrous()).isEqualTo(1);
        assertThat(journal.historique(JournalDossier.objet(d.id()))).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("ANNULATION_DELAI_DEPASSE");
            assertThat(e.avant()).contains("ACCEPTEE");
            assertThat(e.apres()).contains("ANNULEE");
            assertThat(e.acteur().type()).isEqualTo(TypeActeur.SYSTEME);
            assertThat(e.acteur().identifiant()).isEqualTo(ExpirationDossiersService.TRAITEMENT);
            assertThat(e.le()).isEqualTo(HUIT_JOURS_APRES);
        });
        assertThat(evenements).containsExactly(new StatutDossierChange(d.id(), d.clientId(), StatutDossier.ACCEPTEE,
                StatutDossier.ANNULEE, HUIT_JOURS_APRES));
    }

    @Test
    void un_dossier_dans_le_delai_n_est_pas_annule() {
        Demande d = enregistree(StatutDossier.ACCEPTEE);
        ExpirationDossiersService service = service(DemandesFixtures.LE.plus(Duration.ofDays(6)));

        assertThat(service.dossiersADelaiDepasse()).isEmpty();
        assertThat(service.annulerSiDelaiDepasse(d.id())).isFalse();
        assertThat(demandes.parId(d.id()).orElseThrow().statut()).isEqualTo(StatutDossier.ACCEPTEE);
        assertThat(evenements).isEmpty();
    }

    @Test
    void un_dossier_complet_ou_a_un_autre_stade_n_est_pas_annule() {
        Demande complet = enregistree(StatutDossier.APPORT_ET_AUTORISATION);
        Demande enAnalyse = enregistree(StatutDossier.EN_ANALYSE);
        ExpirationDossiersService service = service(HUIT_JOURS_APRES);

        assertThat(service.dossiersADelaiDepasse()).isEmpty();
        assertThat(service.annulerSiDelaiDepasse(complet.id())).isFalse();
        assertThat(service.annulerSiDelaiDepasse(enAnalyse.id())).isFalse();
        assertThat(demandes.parId(complet.id()).orElseThrow().statut()).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
        assertThat(entrees.entrees()).isEmpty();
    }

    @Test
    void un_dossier_inconnu_est_ignore() {
        assertThat(service(HUIT_JOURS_APRES).annulerSiDelaiDepasse(UUID.randomUUID())).isFalse();
    }

    @Test
    void le_delai_est_lu_dans_la_table_parametre() {
        Demande d = enregistree(StatutDossier.ACCEPTEE);
        ParametreService parametres = new ParametreService(
                () -> ParametresDeDepart.avec(CleParametre.DELAI_APPORT_AUTORISATION_JOURS, "10"));
        parametres.afterSingletonsInstantiated();
        ExpirationDossiersService service = service(HUIT_JOURS_APRES, parametres);

        assertThat(service.dossiersADelaiDepasse()).isEmpty();
        assertThat(service.annulerSiDelaiDepasse(d.id())).isFalse();
    }
}
