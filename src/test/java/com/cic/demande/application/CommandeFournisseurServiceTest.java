package com.cic.demande.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.application.JournalEnMemoire;
import com.cic.audit.domain.TypeActeur;
import com.cic.catalogue.application.FournisseurService;
import com.cic.catalogue.application.FournisseursEnMemoire;
import com.cic.catalogue.domain.Fournisseur;
import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.Contrat;
import com.cic.demande.CommandeFournisseurValidee;
import com.cic.demande.StatutDossierChange;
import com.cic.demande.domain.CommandeFournisseur;
import com.cic.demande.domain.ContenuBonCommande;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.DemandesFixtures;
import com.cic.demande.domain.PrealablesManquantsException;
import com.cic.demande.domain.SeparationDesTachesException;
import com.cic.demande.domain.StatutDossier;
import com.cic.demande.domain.TransitionInterditeException;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class CommandeFournisseurServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-15T10:00:00Z");

    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final DemandesEnMemoire demandes = new DemandesEnMemoire();
    private final CommandesEnMemoire commandes = new CommandesEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(new JournalEnMemoire());
    private final List<Object> evenements = new ArrayList<>();
    private final List<ContenuBonCommande> rendus = new ArrayList<>();
    private final PrealablesDeTest prealables = new PrealablesDeTest(clock);
    private final FournisseursEnMemoire fournisseurs = new FournisseursEnMemoire();
    private final Fournisseur fournisseur = Fournisseur.nouveau("Mauritel Store", new BigDecimal("8"));
    private final CommandeFournisseurService service;

    CommandeFournisseurServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        fournisseurs.enregistrer(fournisseur);
        service = new CommandeFournisseurService(demandes, commandes, prealables.service, prealables.contratService,
                new FournisseurService(fournisseurs, parametres), parametres, contenu -> {
                    rendus.add(contenu);
                    return new byte[] {'%', 'P', 'D', 'F'};
                }, journal, evenements::add, clock);
    }

    /** Dossier accepté par « analyste », apport encaissé et autorisation acceptée. */
    private Demande pret() {
        Demande d = DemandesFixtures.decidee(UUID.randomUUID(), DemandesFixtures.termes(fournisseur.id()),
                StatutDossier.APPORT_ET_AUTORISATION, "analyste");
        demandes.enregistrer(d);
        Contrat c = prealables.contrat(d);
        prealables.payerApport(c);
        prealables.accepterAutorisation(c);
        return d;
    }

    private StatutDossier statut(Demande d) {
        return demandes.parId(d.id()).orElseThrow().statut();
    }

    @Test
    void le_paiement_fournisseur_valide_par_une_autre_personne_fait_passer_le_dossier_en_commandee() {
        Demande d = pret();
        Contrat contrat = prealables.contratService.parDemande(d.id()).orElseThrow();

        CommandeFournisseur c = service.commander(d.id(), "admin");

        assertThat(statut(d)).isEqualTo(StatutDossier.COMMANDEE);
        assertThat(c.numero()).isEqualTo("BC-2026-000001");
        assertThat(c.montant()).as("prix d'achat : 20 000 − 8 %").isEqualTo(Montant.de(18_400));
        assertThat(c.fournisseurId()).isEqualTo(fournisseur.id());
        assertThat(c.numeroContrat()).isEqualTo(contrat.numero());
        assertThat(c.validePar()).isEqualTo("admin");
        assertThat(c.valideLe()).isEqualTo(MAINTENANT);
        assertThat(commandes.toutes()).containsExactly(c);
        assertThat(demandes.verrous()).isEqualTo(1);
        assertThat(journal.historique(JournalDossier.objet(d.id()))).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("COMMANDE_FOURNISSEUR");
            assertThat(e.avant()).contains("APPORT_ET_AUTORISATION");
            assertThat(e.apres()).contains("COMMANDEE");
            assertThat(e.acteur().type()).isEqualTo(TypeActeur.BACK_OFFICE);
            assertThat(e.acteur().identifiant()).isEqualTo("admin");
            assertThat(e.detail()).hasValueSatisfying(t -> assertThat(t).contains("BC-2026-000001"));
        });
        assertThat(evenements).containsExactly(
                new StatutDossierChange(d.id(), d.clientId(), StatutDossier.APPORT_ET_AUTORISATION,
                        StatutDossier.COMMANDEE, MAINTENANT),
                new CommandeFournisseurValidee(c.id(), c.numero(), d.id(), contrat.id(), fournisseur.id(),
                        Montant.de(18_400), MAINTENANT));
    }

    @Test
    void l_analyste_qui_a_decide_ne_peut_pas_valider_le_paiement_fournisseur() {
        Demande d = pret();

        assertThatThrownBy(() -> service.commander(d.id(), "analyste"))
                .isInstanceOf(SeparationDesTachesException.class);

        assertThat(statut(d)).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
        assertThat(commandes.toutes()).isEmpty();
        assertThat(evenements).isEmpty();
    }

    @Test
    void jamais_commandee_si_l_autorisation_a_ete_revoquee_entre_temps() {
        Demande d = pret();
        prealables.revoquerAutorisation(prealables.contratService.parDemande(d.id()).orElseThrow());

        assertThatThrownBy(() -> service.commander(d.id(), "admin"))
                .isInstanceOf(PrealablesManquantsException.class);
        assertThat(statut(d)).isEqualTo(StatutDossier.APPORT_ET_AUTORISATION);
        assertThat(commandes.toutes()).isEmpty();
    }

    @Test
    void jamais_commandee_sans_apport_ni_autorisation() {
        Demande d = DemandesFixtures.decidee(UUID.randomUUID(), DemandesFixtures.termes(fournisseur.id()),
                StatutDossier.ACCEPTEE, "analyste");
        demandes.enregistrer(d);
        prealables.contrat(d);

        assertThatThrownBy(() -> service.commander(d.id(), "admin"))
                .isInstanceOf(TransitionInterditeException.class);
        assertThat(statut(d)).isEqualTo(StatutDossier.ACCEPTEE);
        assertThat(commandes.toutes()).isEmpty();
    }

    @Test
    void une_seconde_validation_est_refusee() {
        Demande d = pret();
        service.commander(d.id(), "admin");

        assertThatThrownBy(() -> service.commander(d.id(), "admin2"))
                .isInstanceOf(TransitionInterditeException.class);
        assertThat(commandes.toutes()).hasSize(1);
    }

    @Test
    void la_liste_a_commander_indique_les_prealables_et_qui_peut_valider() {
        Demande d = pret();

        assertThat(service.aCommander()).singleElement().satisfies(a -> {
            assertThat(a.demande().id()).isEqualTo(d.id());
            assertThat(a.nomFournisseur()).isEqualTo("Mauritel Store");
            assertThat(a.analyste()).isEqualTo("analyste");
            assertThat(a.prealables().sontReunis()).isTrue();
            assertThat(a.validablePar("admin")).isTrue();
            assertThat(a.validablePar("analyste")).isFalse();
        });

        service.commander(d.id(), "admin");
        assertThat(service.aCommander()).isEmpty();
        assertThat(service.dernieres(10)).singleElement()
                .satisfies(l -> assertThat(l.nomFournisseur()).isEqualTo("Mauritel Store"));
    }

    @Test
    void le_bon_de_commande_est_disponible_apres_la_validation() {
        Demande d = pret();
        assertThat(service.bonDeCommande(d.id())).isEmpty();

        CommandeFournisseur c = service.commander(d.id(), "admin");

        assertThat(service.bonDeCommande(d.id())).hasValueSatisfying(pdf ->
                assertThat(pdf.nomFichier()).isEqualTo("bon-de-commande-BC-2026-000001.pdf"));
        assertThat(rendus).singleElement().satisfies(r -> {
            assertThat(r.commande()).isEqualTo(c);
            assertThat(r.nomFournisseur()).isEqualTo("Mauritel Store");
            assertThat(r.remise()).isEqualTo(Montant.de(1_600));
            assertThat(r.raisonSociale()).isEqualTo("Seheli");
        });
    }
}
